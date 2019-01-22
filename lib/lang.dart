import 'dart:async';
import 'dart:convert';

import 'package:flutter/cupertino.dart' show CupertinoLocalizations, DefaultCupertinoLocalizations;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show rootBundle;

///
/// https://flutter-news.com/tutorials/flutter-internationalization-by-using-json-files.5
///
class Lang {
  Lang(this.locale);

  final Locale locale;

  static Lang of(BuildContext context) {
    return Localizations.of<Lang>(context, Lang);
  }

  Map<String, String> _sentences;

  static const List<String> _languageMap = ['en', 'zh_TW'];

  static String _getLangFileName(Locale locale) {
    String name = '${locale.languageCode}_${locale.countryCode}';
    return _languageMap.contains(name) ? name : locale.languageCode;
  }

  static bool isSupported(Locale locale) {
    return _languageMap.contains(_getLangFileName(locale));
  }

  Future<bool> load() async {
    String name = 'assets/strings/${_getLangFileName(locale).toLowerCase()}.json';
    print('json name: $name');
    String data = await rootBundle.loadString(name);
    Map<String, dynamic> _result = json.decode(data);

    this._sentences = new Map();
    _result.forEach((String key, dynamic value) {
      this._sentences[key] = value.toString();
    });

    return true;
  }

  String trans(String key) {
    return (_sentences != null && _sentences.containsKey(key) ? this._sentences[key] : key) ?? key;
  }

  String getLanguageKey() => _getLangFileName(locale);
}

class LangLocalizationsDelegate extends LocalizationsDelegate<Lang> {
  const LangLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) {
    //print('support locale ${locale.languageCode} ${locale.countryCode}');
    return Lang.isSupported(locale);
  }

  @override
  Future<Lang> load(Locale locale) async {
    Lang localizations = new Lang(locale);
    //load large json file in localizationsDelegates will cause black screen
    //await localizations.load();
    print("Load ${locale.languageCode} ${locale.countryCode}");
    return localizations;
  }

  @override
  bool shouldReload(LangLocalizationsDelegate old) => false;
}

/// https://github.com/flutter/flutter/issues/23047#issuecomment-436882321
class FallbackCupertinoLocalisationsDelegate extends LocalizationsDelegate<CupertinoLocalizations> {
  const FallbackCupertinoLocalisationsDelegate();

  @override
  bool isSupported(Locale locale) => true;

  @override
  Future<CupertinoLocalizations> load(Locale locale) => DefaultCupertinoLocalizations.load(locale);

  @override
  bool shouldReload(FallbackCupertinoLocalisationsDelegate old) => false;
}
