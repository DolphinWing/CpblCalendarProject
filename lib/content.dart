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
    return new Container(
      padding: EdgeInsets.only(left: 16, right: 16, top: 2, bottom: 2),
      child: Column(
        children: <Widget>[
          //Divider(),
          Row(
            children: <Widget>[
              Text(game.getDisplayTime()),
              Expanded(child: SizedBox()),
              Text('${game.getGameType(context)} ${game.id}'),
            ],
          ),
          Row(
            children: <Widget>[
              Expanded(child: Text(game.home.getDisplayName(context))),
              Text('${game.home.score}'),
            ],
          ),
          Row(
            children: <Widget>[
              Expanded(child: Text(game.away.getDisplayName(context))),
              Text('${game.away.score}'),
            ],
          ),
          Row(
            children: <Widget>[
              Text(game.getFieldName(context)),
            ],
          ),
          game.extra != null ? Text('${game.extra}') : SizedBox(height: 1),
          //Divider(),
        ],
      ),
    );
  }

  Widget buildContent(BuildContext context, List<Game> list, int mode) {
    List<Widget> widgetList = new List();
    list?.forEach((game) {
      widgetList.add(buildGameWidget(context, game));
    });
    return ListView(
      children: widgetList,
    );
  }

  @override
  Widget build(BuildContext context) {
    return widget.loading
        ? Stack(
            alignment: Alignment.center,
            children: <Widget>[
              buildContent(context, widget.list, widget.mode),
              Positioned(child: CircularProgressIndicator(), top: 200),
            ],
          )
        : buildContent(context, widget.list, widget.mode);
  }
}
