package dolphin.android.apps.CpblCalendar.provider;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Game {
    public int Id = 0;
    public String Kind = null;

    public Team HomeTeam = null;
    public int HomeScore = 0;

    public Team AwayTeam = null;
    public int AwayScore = 0;

    public String Field = null;
    //public int FieldId;

    public Calendar StartTime;
    public boolean IsFinal = false;
    public String Url = null;//link to final website

    public String Channel = null;

    public boolean IsDelay = false;
    public String DelayMessage = null;

    public int Source = SOURCE_CPBL;

    public final static int SOURCE_CPBL = 0;
    public final static int SOURCE_ZXC22 = 1;
    public final static int SOURCE_CPBL_2013 = 2;

    public int People = 0;

    /**
     * convert from Game object to string
     *
     * @param game game
     * @return string data
     */
    public static String toPrefString(Game game) {
        return String.format("%d;%d;%d;%s;%s;%d;%s", game.Id,
                game.AwayTeam.getId(), game.HomeTeam.getId(),
                game.Field, game.Channel, game.StartTime.getTimeInMillis(), game.Kind);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    /**
     * convert from string to Game object
     */
    public static Game fromPrefString(Context context, String str) {
        String[] gInfo = str.split(";");
        //Log.d("dolphin", "size: " + gInfo.length);
        //for (String s : gInfo)
        //    Log.d("dolphin", " " + s);
        Game game = new Game();
        game.Id = Integer.decode(gInfo[0]);
        game.AwayTeam = new Team(context, Integer.decode(gInfo[1]));
        game.HomeTeam = new Team(context, Integer.decode(gInfo[2]));
        game.Field = gInfo[3];
        game.Channel = (gInfo[4].equals("null") || gInfo[4].trim().isEmpty()) ? null : gInfo[4];
        game.StartTime = Calendar.getInstance();
        game.StartTime.setTimeInMillis(Long.decode(gInfo[5]));
        game.Kind = gInfo.length < 6 ? "01" : gInfo[6];//[122]dolphin++ add Kind
        return game;
    }

    //http://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/com/google/gson/TypeAdapter.html
    public static class GameTypeAdapter extends TypeAdapter<Game> {
        private Context mContext;

        public GameTypeAdapter(Context context) {
            super();
            mContext = context;
        }

        @Override
        public void write(JsonWriter jsonWriter, Game game) throws IOException {
            if (game == null) {
                jsonWriter.nullValue();
                return;
            }

            writeOneGame(jsonWriter, game);
        }

        public static void writeOneGame(JsonWriter jsonWriter, Game game) throws IOException {
            jsonWriter.beginObject();

            jsonWriter.name("Id").value(game.Id);
            jsonWriter.name("Kind").value(game.Kind);
            jsonWriter.name("StartTime").value(game.StartTime.getTimeInMillis());

            jsonWriter.name("AwayTeamId").value(game.AwayTeam.getId());
            if (game.AwayTeam.getId() == Team.ID_UNKNOWN)
                jsonWriter.name("AwayTeamName").value(game.AwayTeam.getName());
            jsonWriter.name("HomeTeamId").value(game.HomeTeam.getId());
            if (game.HomeTeam.getId() == Team.ID_UNKNOWN)
                jsonWriter.name("HomeTeamName").value(game.HomeTeam.getName());

            jsonWriter.name("Source").value(game.Source);
            if (game.Source == Game.SOURCE_ZXC22)
                jsonWriter.name("People").value(game.People);

            jsonWriter.name("IsFinal").value(game.IsFinal);
            if (game.IsFinal) {
                jsonWriter.name("AwayScore").value(game.AwayScore);
                jsonWriter.name("HomeScore").value(game.HomeScore);
            }

            if (game.Field != null)
                jsonWriter.name("Field").value(game.Field);
            if (game.Channel != null)
                jsonWriter.name("Channel").value(game.Channel);
            if (game.Url != null)
                jsonWriter.name("Url").value(game.Url);

            jsonWriter.name("IsDelay").value(game.IsDelay);
            if (game.IsDelay) {
                jsonWriter.name("DelayMessage").value(game.DelayMessage);
            }
            jsonWriter.endObject();
        }

        @Override
        public Game read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return readOneGame(mContext, jsonReader);
        }

        public static Game readOneGame(Context context, JsonReader jsonReader) throws IOException {
            Game game = new Game();
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if (name.equals("Id")) {
                    game.Id = jsonReader.nextInt();
                } else if (name.equals("Kind")) {
                    game.Kind = jsonReader.nextString();
                } else if (name.equals("StartTime")) {
                    game.StartTime = CpblCalendarHelper.getNowTime();
                    game.StartTime.setTimeInMillis(jsonReader.nextLong());
                } else if (name.equals("Source")) {
                    game.Source = jsonReader.nextInt();
                } else if (name.equals("Field")) {
                    game.Field = jsonReader.nextString();
                } else if (name.equals("AwayTeamId")) {
                    game.AwayTeam = new Team(context, jsonReader.nextInt());
                } else if (name.equals("HomeTeamId")) {
                    game.HomeTeam = new Team(context, jsonReader.nextInt());
                } else if (name.equals("IsFinal")) {
                    game.IsFinal = jsonReader.nextBoolean();
                } else if (name.equals("AwayScore")) {
                    game.AwayScore = jsonReader.nextInt();
                } else if (name.equals("HomeScore")) {
                    game.HomeScore = jsonReader.nextInt();
                } else if (name.equals("People")) {
                    game.People = jsonReader.nextInt();
                } else if (name.equals("Url")) {
                    game.Url = jsonReader.nextString();
                } else if (name.equals("IsDelay")) {
                    game.IsDelay = jsonReader.nextBoolean();
                } else if (name.equals("Kind")) {
                    game.Kind = jsonReader.nextString();
                } else if (name.equals("Channel")) {
                    game.Channel = jsonReader.nextString();
                } else if (name.equals("DelayMessage")) {
                    game.DelayMessage = jsonReader.nextString();
                } else if (name.equals("HomeTeamName")) {//this may override the HomeTeam
                    game.HomeTeam = new Team(context, jsonReader.nextString(), 2014);
                } else if (name.equals("AwayTeamName")) {//this may override the AwayTeam
                    game.AwayTeam = new Team(context, jsonReader.nextString(), 2014);
                }
            }
            jsonReader.endObject();
            return game;
        }
    }

    public static String toJson(Context context, Game game) {
        Type type = new TypeToken<Game>() {
        }.getType();
        Gson gson = new GsonBuilder().registerTypeAdapter(type, new GameTypeAdapter(context)).create();
        //Log.d(TAG, gson.toJson(game));
        return gson.toJson(game);
    }

    public static Game fromJson(Context context, String json) {
        Type type = new TypeToken<Game>() {
        }.getType();
        Gson gson = new GsonBuilder().registerTypeAdapter(type, new GameTypeAdapter(context)).create();
        return gson.fromJson(json, type);
    }

    public static class GameListTypeAdapter extends TypeAdapter<ArrayList<Game>> {
        private Context mContext;

        public GameListTypeAdapter(Context context) {
            super();
            mContext = context;
        }

        @Override
        public void write(JsonWriter jsonWriter, ArrayList<Game> games) throws IOException {
            if (games == null) {
                jsonWriter.nullValue();
                return;
            }

            jsonWriter.beginArray();
            for (Game game : games) {
                GameTypeAdapter.writeOneGame(jsonWriter, game);
            }
            jsonWriter.endArray();
        }

        @Override
        public ArrayList<Game> read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }

            ArrayList<Game> list = new ArrayList<Game>();
            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                list.add(GameTypeAdapter.readOneGame(mContext, jsonReader));
            }
            jsonReader.endArray();
            return list;
        }
    }

    public static String listToJson(Context context, ArrayList<Game> games) {
        //Type type = new TypeToken<List<Game>>() {
        //}.getType();
        Gson gson = new GsonBuilder().registerTypeAdapter(ArrayList.class, new GameListTypeAdapter(context)).create();
        //Log.d(TAG, gson.toJson(games));
        return gson.toJson(games);
    }

    public static ArrayList<Game> listFromJson(Context context, String json) {
        Type type = new TypeToken<List<Game>>() {
        }.getType();
        Gson gson = new GsonBuilder().registerTypeAdapter(type, new GameListTypeAdapter(context)).create();
        return gson.fromJson(json, type);
    }
}
