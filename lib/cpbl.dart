
class Team {
  int id;
  int name;
  int score;
  int color;
}

class Game {
  int id;
  int kind;

  Team home;
  Team away;

  int fieldId;
  DateTime time;

  String extra;
}