import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../features/home/home_screen.dart';

class IPTVPlayerApp extends StatelessWidget {
  const IPTVPlayerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      title: 'IPTV Player',
      debugShowCheckedModeBanner: false,
      theme: _buildTheme(),
      home: const HomeScreen(),
    );
  }

  ThemeData _buildTheme() {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      colorScheme: const ColorScheme.dark(
        primary: Color(0xFF00E5FF),
        secondary: Color(0xFF1DE9B6),
        surface: Color(0xFF0A0E1A),
        error: Color(0xFFFF5252),
      ),
      scaffoldBackgroundColor: const Color(0xFF0A0E1A),
      fontFamily: 'monospace',
    );
  }
}
