import 'package:flutter/material.dart';
import 'cpbl.dart';

class UiMode {
  static const int list = 0;
  static const int quick = 1;
}

class ContentUiWidget extends StatefulWidget {
  final bool loading;
  final List<Game> list;
  final int mode;

  ContentUiWidget({this.loading = false, List<Game> list, this.mode = UiMode.list})
      : this.list = list ?? new List();

  @override
  State<StatefulWidget> createState() => _ContentUiWidgetState();
}

class _ContentUiWidgetState extends State<ContentUiWidget> {
  Widget buildGameWidget(BuildContext context, Game game) {
    return Text('');
  }

  Widget buildContent(BuildContext context, List<Game> list, int mode) {
    return ListView();
  }

  @override
  Widget build(BuildContext context) {
    return widget.loading
        ? Stack(
            children: <Widget>[
              buildContent(context, widget.list, widget.mode),
              Positioned(child: CircularProgressIndicator(), top: 200),
            ],
          )
        : buildContent(context, widget.list, widget.mode);
  }
}
