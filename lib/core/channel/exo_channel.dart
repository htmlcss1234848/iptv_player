import 'package:flutter/services.dart';
import '../models/stream_url_model.dart';
import '../models/player_state_model.dart';
import '../models/debug_info_model.dart';

/// Flutter ↔ Native ExoPlayer bridge
/// All communication with Android ExoPlayer goes through here
class ExoChannel {
  static const _methodChannel =
      MethodChannel('com.iptvplayer/exoplayer');
  static const _eventChannel =
      EventChannel('com.iptvplayer/exoplayer_events');

  // Singleton
  static final ExoChannel _instance = ExoChannel._internal();
  factory ExoChannel() => _instance;
  ExoChannel._internal();

  Stream<Map<dynamic, dynamic>>? _eventStream;

  // ─── Commands → Native ────────────────────────────────────────────────────

  Future<void> play(StreamUrlModel streamUrl) async {
    await _methodChannel.invokeMethod('play', streamUrl.toMap());
  }

  Future<void> pause() async {
    await _methodChannel.invokeMethod('pause');
  }

  Future<void> resume() async {
    await _methodChannel.invokeMethod('resume');
  }

  Future<void> stop() async {
    await _methodChannel.invokeMethod('stop');
  }

  Future<void> seekTo(Duration position) async {
    await _methodChannel.invokeMethod('seekTo', {
      'positionMs': position.inMilliseconds,
    });
  }

  Future<void> setVolume(double volume) async {
    await _methodChannel.invokeMethod('setVolume', {'volume': volume});
  }

  Future<void> dispose() async {
    await _methodChannel.invokeMethod('dispose');
  }

  Future<void> retry() async {
    await _methodChannel.invokeMethod('retry');
  }

  Future<PlayerStateModel> getState() async {
    final result = await _methodChannel.invokeMethod<Map>('getState');
    if (result == null) return const PlayerStateModel();
    return PlayerStateModel.fromMap(result);
  }

  Future<DebugInfoModel> getDebugInfo() async {
    final result = await _methodChannel.invokeMethod<Map>('getDebugInfo');
    if (result == null) return DebugInfoModel.empty();
    return DebugInfoModel.fromMap(result);
  }

  // ─── Events ← Native ──────────────────────────────────────────────────────

  Stream<Map<dynamic, dynamic>> get eventStream {
    _eventStream ??= _eventChannel
        .receiveBroadcastStream()
        .cast<Map<dynamic, dynamic>>();
    return _eventStream!;
  }

  Stream<PlayerStateModel> get stateStream {
    return eventStream
        .where((e) => e['type'] == 'state')
        .map((e) => PlayerStateModel.fromMap(e));
  }

  Stream<DebugInfoModel> get debugStream {
    return eventStream
        .where((e) => e['type'] == 'debug')
        .map((e) => DebugInfoModel.fromMap(e));
  }

  Stream<String> get errorStream {
    return eventStream
        .where((e) => e['type'] == 'error')
        .map((e) => e['message'] as String? ?? 'Unknown error');
  }
}
