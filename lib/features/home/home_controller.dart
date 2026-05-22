import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../core/utils/url_parser.dart';
import '../../core/models/stream_url_model.dart';
import '../player/player_screen.dart';

class HomeController extends GetxController {
  final urlController = TextEditingController();
  final RxList<String> recentUrls = <String>[].obs;
  final RxBool isValidUrl = false.obs;

  static const _prefsKey = 'recent_urls';
  static const _maxRecent = 10;

  @override
  void onInit() {
    super.onInit();
    _loadRecentUrls();
    urlController.addListener(_onUrlChanged);
  }

  @override
  void onClose() {
    urlController.dispose();
    super.onClose();
  }

  void _onUrlChanged() {
    final url = urlController.text.trim();
    isValidUrl.value = url.isNotEmpty &&
        (url.startsWith('http://') || url.startsWith('https://'));
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

  void clearInput() {
    urlController.clear();
  }

  void pasteFromClipboard() async {
    // Gets text from clipboard via Flutter
    final data = await _getClipboardText();
    if (data != null && data.isNotEmpty) {
      urlController.text = data;
    }
  }

  Future<String?> _getClipboardText() async {
    // Using Flutter's Clipboard
    final value = await _clipboardRead();
    return value;
  }

  Future<String?> _clipboardRead() async {
    try {
      final data = await ServicesBinding.instance.keyboard.toString();
      return null; // placeholder - actual impl via Clipboard.getData
    } catch (_) {
      return null;
    }
  }

  void _navigateToPlayer(String url) {
    final streamUrl = UrlParser.parse(url);
    // Add default headers if needed
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
    recentUrls.remove(url); // remove duplicate
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
