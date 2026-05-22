import 'package:flutter/material.dart';

class ErrorOverlay extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;
  final VoidCallback onBack;

  const ErrorOverlay({
    super.key,
    required this.message,
    required this.onRetry,
    required this.onBack,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.black87,
      child: Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.error_outline,
                color: Color(0xFFFF5252),
                size: 56,
              ),
              const SizedBox(height: 16),
              const Text(
                'PLAYBACK ERROR',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 2,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                message,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: Colors.white54,
                  fontSize: 13,
                  fontFamily: 'monospace',
                ),
              ),
              const SizedBox(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  OutlinedButton(
                    onPressed: onBack,
                    style: OutlinedButton.styleFrom(
                      side: const BorderSide(color: Colors.white30),
                      foregroundColor: Colors.white70,
                    ),
                    child: const Text('BACK'),
                  ),
                  const SizedBox(width: 16),
                  ElevatedButton.icon(
                    onPressed: onRetry,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF00E5FF),
                      foregroundColor: Colors.black,
                    ),
                    icon: const Icon(Icons.refresh, size: 18),
                    label: const Text(
                      'RETRY',
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
