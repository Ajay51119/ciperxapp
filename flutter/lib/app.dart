import 'package:flutter/material.dart';
import 'features/home/home_screen.dart';
import 'state/cipher_store.dart';

class CipherApp extends StatefulWidget {
  const CipherApp({super.key});

  @override
  State<CipherApp> createState() => _CipherAppState();
}

class _CipherAppState extends State<CipherApp> {
  late final CipherStore store = CipherStore()..bootstrap();

  @override
  void dispose() {
    store.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(
        seedColor: const Color(0xFF1DB954),
        brightness: Brightness.dark,
      ),
      scaffoldBackgroundColor: const Color(0xFF0F1115),
    );

    return AnimatedBuilder(
      animation: store,
      builder: (context, _) {
        return MaterialApp(
          debugShowCheckedModeBanner: false,
          theme: theme,
          home: HomeScreen(store: store),
        );
      },
    );
  }
}
