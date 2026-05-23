import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get/get.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../core/utils/url_parser.dart';
import '../../core/models/stream_url_model.dart';
import '../player/player_screen.dart';

class HomeController extends GetxController {
  final urlController = TextEditingController();
  final RxList<String> recentUrls = <String>[].obs;

  static const _prefsKey = 'recent_urls';
  static const _maxRecent = 10;

  @override
  void onInit() {
    super.onInit();
    _loadRecentUrls();
  }

  @override
  void onClose() {
    urlController.dispose();
    super.onClose();
  }

  void onPlay() {
    final url = urlController.text.trim();
    if (url.isEmpty) return;
    _saveRecentUrl(url);
    _navigateToPlayer(url);
  }

  void onRecentUrlTap(String url) {
    urlController.text = url;
    _navigateToPlayer(url);
  }

  void onRecentUrlDelete(String url) {
    recentUrls.remove(url);
    _persistRecentUrls();
  }

  void clearInput() => urlController.clear();

  Future<void> pasteFromClipboard() async {
    final data = await Clipboard.getData('text/plain');
    if (data?.text != null && data!.text!.isNotEmpty) {
      urlController.text = data.text!;
    }
  }

  void _navigateToPlayer(String url) {
    final streamUrl = UrlParser.parse(url);
    final headers = UrlParser.defaultHeadersFor(url);
    final finalStream = headers.isNotEmpty
        ? streamUrl.copyWith(headers: headers)
        : streamUrl;

    Get.to(
      () => PlayerScreen(streamUrl: finalStream),
      transition: Transition.fadeIn,
    );
  }

  Future<void> _saveRecentUrl(String url) async {
    recentUrls.remove(url);
    recentUrls.insert(0, url);
    if (recentUrls.length > _maxRecent) {
      recentUrls.removeRange(_maxRecent, recentUrls.length);
    }
    await _persistRecentUrls();
  }

  Future<void> _loadRecentUrls() async {
    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getStringList(_prefsKey) ?? [];
    recentUrls.assignAll(saved);
  }

  Future<void> _persistRecentUrls() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setStringList(_prefsKey, recentUrls.toList());
  }
}
