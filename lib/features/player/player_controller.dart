import 'dart:async';
import 'package:flutter/services.dart';
import 'package:get/get.dart';
import '../../core/channel/exo_channel.dart';
import '../../core/models/stream_url_model.dart';
import '../../core/models/player_state_model.dart';
import '../../core/models/debug_info_model.dart';

class PlayerController extends GetxController {
  final StreamUrlModel streamUrl;
  final _channel = ExoChannel();

  final Rx<PlayerStateModel> playerState =
      Rx(const PlayerStateModel());
  final Rx<DebugInfoModel> debugInfo = Rx(DebugInfoModel.empty());
  final RxBool showControls = true.obs;
  final RxBool showDebug = false.obs;
  final RxBool isFullscreen = false.obs;

  StreamSubscription? _stateSub;
  StreamSubscription? _debugSub;
  StreamSubscription? _errorSub;
  Timer? _hideControlsTimer;

  PlayerController({required this.streamUrl});

  @override
  void onInit() {
    super.onInit();
    _subscribeToEvents();
    _startPlayback();
    _scheduleHideControls();
    _setLandscape();
  }

  @override
  void onClose() {
    _stateSub?.cancel();
    _debugSub?.cancel();
    _errorSub?.cancel();
    _hideControlsTimer?.cancel();
    _channel.dispose();
    _restoreOrientation();
    super.onClose();
  }

  // ─── Playback controls ────────────────────────────────────────────────────

  Future<void> _startPlayback() async {
    playerState.value = playerState.value.copyWith(
      state: PlayerState.initializing,
    );
    await _channel.play(streamUrl);
  }

  Future<void> togglePlayPause() async {
    if (playerState.value.isPlaying) {
      await _channel.pause();
    } else {
      await _channel.resume();
    }
    _resetHideControlsTimer();
  }

  Future<void> retry() async {
    playerState.value =
        playerState.value.copyWith(state: PlayerState.initializing);
    await _channel.retry();
  }

  Future<void> stop() async {
    await _channel.stop();
  }

  Future<void> seekTo(Duration position) async {
    await _channel.seekTo(position);
    _resetHideControlsTimer();
  }

  // ─── UI controls ──────────────────────────────────────────────────────────

  void onTapPlayer() {
    if (showControls.value) {
      showControls.value = false;
      _hideControlsTimer?.cancel();
    } else {
      showControls.value = true;
      _scheduleHideControls();
    }
  }

  void toggleDebug() {
    showDebug.toggle();
    _resetHideControlsTimer();
  }

  void toggleFullscreen() {
    isFullscreen.toggle();
    if (isFullscreen.value) {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    } else {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    }
    _resetHideControlsTimer();
  }

  void _scheduleHideControls() {
    _hideControlsTimer?.cancel();
    _hideControlsTimer = Timer(const Duration(seconds: 4), () {
      if (playerState.value.isPlaying) {
        showControls.value = false;
      }
    });
  }

  void _resetHideControlsTimer() {
    showControls.value = true;
    _scheduleHideControls();
  }

  // ─── Event subscriptions ─────────────────────────────────────────────────

  void _subscribeToEvents() {
    _stateSub = _channel.stateStream.listen((state) {
      playerState.value = state;
    });

    _debugSub = _channel.debugStream.listen((debug) {
      debugInfo.value = debug;
    });

    _errorSub = _channel.errorStream.listen((error) {
      playerState.value = playerState.value.copyWith(
        state: PlayerState.error,
        errorMessage: error,
      );
    });
  }

  // ─── Orientation ─────────────────────────────────────────────────────────

  void _setLandscape() {
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
  }

  void _restoreOrientation() {
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.portraitUp,
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
  }
}
