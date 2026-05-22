import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Embeds the native Android ExoPlayer view inside Flutter
class PlayerView extends StatelessWidget {
  const PlayerView({super.key});

  @override
  Widget build(BuildContext context) {
    return AndroidView(
      viewType: 'com.iptvplayer/player_view',
      layoutDirection: TextDirection.ltr,
      creationParams: const {},
      creationParamsCodec: const StandardMessageCodec(),
    );
  }
}
