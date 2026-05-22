import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../../core/models/stream_url_model.dart';
import '../../core/models/player_state_model.dart';
import 'player_controller.dart';
import 'widgets/player_view.dart';
import 'widgets/controls_overlay.dart';
import 'widgets/debug_overlay.dart';
import 'widgets/error_overlay.dart';

class PlayerScreen extends StatelessWidget {
  final StreamUrlModel streamUrl;

  const PlayerScreen({super.key, required this.streamUrl});

  @override
  Widget build(BuildContext context) {
    final controller = Get.put(
      PlayerController(streamUrl: streamUrl),
      tag: streamUrl.resolvedUrl,
    );

    return PopScope(
      onPopInvoked: (_) {
        Get.delete<PlayerController>(tag: streamUrl.resolvedUrl);
      },
      child: Scaffold(
        backgroundColor: Colors.black,
        body: GestureDetector(
          onTap: controller.onTapPlayer,
          child: Stack(
            fit: StackFit.expand,
            children: [
              // ── Native ExoPlayer Surface ──────────────────────────
              const PlayerView(),

              // ── Debug overlay (top-left) ──────────────────────────
              Obx(() {
                if (!controller.showDebug.value) return const SizedBox();
                return DebugOverlay(info: controller.debugInfo.value);
              }),

              // ── Error overlay ─────────────────────────────────────
              Obx(() {
                final state = controller.playerState.value;
                if (!state.hasError) return const SizedBox();
                return ErrorOverlay(
                  message: state.errorMessage ?? 'Unknown error',
                  onRetry: controller.retry,
                  onBack: () => Get.back(),
                );
              }),

              // ── Controls overlay ──────────────────────────────────
              Obx(() {
                if (!controller.showControls.value) return const SizedBox();
                final state = controller.playerState.value;
                if (state.hasError) return const SizedBox();
                return ControlsOverlay(
                  state: state,
                  onPlayPause: controller.togglePlayPause,
                  onToggleDebug: controller.toggleDebug,
                  onBack: () => Get.back(),
                  onToggleFullscreen: controller.toggleFullscreen,
                  onSeek: controller.seekTo,
                  isFullscreen: controller.isFullscreen.value,
                  debugOn: controller.showDebug.value,
                );
              }),

              // ── Buffering spinner (when controls hidden) ──────────
              Obx(() {
                final state = controller.playerState.value;
                final buffering = state.isBuffering ||
                    state.state == PlayerState.initializing;
                if (!buffering || controller.showControls.value) {
                  return const SizedBox();
                }
                return const Center(
                  child: SizedBox(
                    width: 40,
                    height: 40,
                    child: CircularProgressIndicator(
                      color: Color(0xFF00E5FF),
                      strokeWidth: 2.5,
                    ),
                  ),
                );
              }),
            ],
          ),
        ),
      ),
    );
  }
}
