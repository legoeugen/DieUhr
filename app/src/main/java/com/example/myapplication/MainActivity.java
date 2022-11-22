package com.example.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {

    private TextView WochentagText;
    private TextView ZeitText;
    private TextView CurrentLessonText;
    private TextView CurrentRoomText;
    private TextView LessonStartText;
    private TextView LessonEndText;
    private TextView NextLessonText;
    private TextView NextRoomText;
    private TextView NextTextText;

    private ProgressBar LessonProgressBar;

    private View LessonDivider;

    private int hour;
    private int min;
    private int sek;

    final static String[] DAYS = {
            "Montag","Dinstag","Mittwoch","Donnerstag","Freitag","Samstag","Sonntag"
    };

    String currentLessonplan;

    Thread t1 = new Thread(() -> StartClock());

    public List<Lesson> dayLessons = new ArrayList<>();

    private int currentHour;
    private String StartTime;
    private String EndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());

        setContentView(R.layout.activity_main);

        WochentagText = (TextView) findViewById(R.id.Wochentag);
        ZeitText = (TextView) findViewById(R.id.Zeit);
        CurrentLessonText = (TextView) findViewById(R.id.Lesson);
        CurrentRoomText = (TextView) findViewById(R.id.current_Room);
        LessonStartText = (TextView) findViewById(R.id.LessonStart);
        LessonEndText = (TextView) findViewById(R.id.LessonEnd);
        NextLessonText = (TextView) findViewById(R.id.NextLesson);
        NextRoomText = (TextView) findViewById(R.id.NextRoom);
        LessonProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        LessonDivider = (View) findViewById(R.id.NextLessonDivider);
        NextTextText = (TextView) findViewById(R.id.NextText);

        GetDate();

        GetCurrentLessonTest(9,30,true);

        t1.start();
    }

    private void GetDate(){
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/YYYY");
        String[] dates = sdf.format(date).split("/");
        int d = Integer.parseInt(dates[0]);
        int m = Integer.parseInt(dates[1]);
        int y = Integer.parseInt(dates[2]);

        if (m < 3){
            m += 12;
            y -= 1;
        }

        int k = y % 100;
        int j = y / 100;

        int day = ((d + (((m + 1) * 26) / 10) + k + (k / 4) + (j / 4)) % 7);

        WochentagText.setText(DAYS[day]);
        SetCurrentLessonplan(DAYS[day]);
    }

    private void SetCurrentLessonplan(String day){
        try {

            if (day == "Samstag" || day == "Sonntag")
                currentLessonplan = "Montag";
            else
                currentLessonplan = day;

            InputStream is = getAssets().open(currentLessonplan + ".txt");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                while (reader.ready()) {
                    String line = reader.readLine();

                    String[] result = line.split(",");

                    Lesson newLesson = new Lesson(result[0],result[1]);

                    dayLessons.add(newLesson);
                }
            }catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (Exception e){
            e.printStackTrace();
        }


    }

    private void StartClock(){
        try {
            // create a clock
            ZoneId zid = ZoneId.of("Europe/Paris");

            while (true) {
                LocalDateTime lt
                        = LocalDateTime.now(zid);

                hour = lt.getHour();
                min = lt.getMinute();
                sek = lt.getSecond();

                ZeitText.setText(hour + ":" + min + ":" + sek);

                if (currentLessonplan != "Mittwoch") { //TODO activate this but not every second
                    GetCurrentLessonTest(hour, min, false);
                }else {
                    GetCurrentLessonTest(hour,min,true);
                }

                LessonProgressBar.setMax(Integer.parseInt(EndTime));
                LessonProgressBar.setMin(Integer.parseInt(StartTime));
                String timeForProgressBar;
                if (min >= 10) {
                    timeForProgressBar = hour + "" + min;
                } else {
                    timeForProgressBar = hour + "0" + min;
                }
                LessonProgressBar.setProgress(Integer.parseInt(timeForProgressBar));

                try {
                    t1.sleep(500);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void GetCurrentLessonTest(int hour, int minute,boolean wednesday){
        try{
            InputStream is;
            if (!wednesday) {
                is = getAssets().open("HourData.txt");
            }
            else {
                is = getAssets().open("HourDataWednesday.txt");
            }
            if (is == null){
                System.out.println("NULL");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                while (reader.ready()) {
                    String line = reader.readLine();
                    String[] result = line.split("-"); //z.b. 07:30-08:15 => 07:30 && 08:15
                    String[] result2 = result[0].split(":"); //z.b. 07:30 => 07 && 30
                    String[] result3 = result[1].split(":"); //z.b. 08:15 => 08 && 15

                    String string1 = result[0] + ":00";
                    Date time1 = new SimpleDateFormat("HH:mm:ss").parse(string1);
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.setTime(time1);
                    calendar1.add(Calendar.DATE, 1);


                    String string2 = result[1] + ":00";
                    Date time2 = new SimpleDateFormat("HH:mm:ss").parse(string2);
                    Calendar calendar2 = Calendar.getInstance();
                    calendar2.setTime(time2);
                    calendar2.add(Calendar.DATE, 1);

                    String someRandomTime = hour + ":" + minute + ":00";
                    Date d = new SimpleDateFormat("HH:mm:ss").parse(someRandomTime);
                    Calendar calendar3 = Calendar.getInstance();
                    calendar3.setTime(d);
                    calendar3.add(Calendar.DATE, 1);

                    Date x = calendar3.getTime();
                    if (x.after(calendar1.getTime()) && x.before(calendar2.getTime())) {
                        System.out.println(Integer.parseInt(result2[0]));
                        StartTime = result2[0].replaceFirst("^0+(?!$)", "") + "" + result2[1];
                        EndTime = result3[0].replaceFirst("^0+(?!$)", "") + "" + result3[1];

                        switch (Integer.parseInt(result2[0])){
                            case 7:
                                currentHour = 1;
                                break;
                            case 8:
                                currentHour = 2;
                                break;
                            case 9:
                                currentHour = 3;
                                break;
                            case 10:
                                currentHour = 4;
                                break;
                            case 11:
                                currentHour = 5;
                                break;
                            case 12:
                                currentHour = 6;
                                break;
                            case 13:
                                currentHour = 7;
                                break;
                            case 14:
                                currentHour = 8;
                                break;
                            case 15:
                                currentHour = 9;
                                break;
                        }

                        System.out.println("get current lesson and stuff with current hour: " + currentHour);

                        Lesson currentLesson;
                        Lesson nextLesson;

                        try {
                            currentLesson = dayLessons.get(currentHour - 1);
                            System.out.println("current Lesson: " + currentLesson.Lesson);
                            LessonDivider.setVisibility(View.VISIBLE);
                            NextTextText.setVisibility(View.VISIBLE);

                        }catch (Exception e){
                            System.out.println("keine lessons mehr");
                            UpdateUiEndOfSchool();
                            LessonDivider.setVisibility(View.INVISIBLE);
                            NextTextText.setVisibility(View.INVISIBLE);
                            return;
                        }

                        try{
                            nextLesson = dayLessons.get(currentHour);
                            LessonDivider.setVisibility(View.VISIBLE);
                            NextTextText.setVisibility(View.VISIBLE);
                        } catch (Exception e){
                            System.out.println("Keine nächste/n stunde/n");
                            nextLesson = new Lesson("","");
                            LessonDivider.setVisibility(View.INVISIBLE);
                            NextTextText.setVisibility(View.INVISIBLE);
                        }

                        UpdateUI(result[0], result[1], currentLesson.Lesson, currentLesson.Room, nextLesson.Lesson, nextLesson.Room);

                        break;
                    }
                    else {
                        UpdateUiPause();
                    }
                }
            }catch (FileNotFoundException e) {
                System.out.println("Null");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Null");
                e.printStackTrace();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void UpdateUiEndOfSchool(){
        System.out.println("End of school for today");
        CurrentLessonText.setText("End!");
        CurrentRoomText.setTextSize(50);
        CurrentRoomText.setText("End!");
        LessonStartText.setText("");
        LessonEndText.setText("");
        NextLessonText.setText("");
        NextRoomText.setText("");
    }

    private void UpdateUI(String startTime,String endTime,String Lesson,String Room,String NextLesson,String NextRoom){
        System.out.println("Update UI with: " + startTime + "," + endTime + "," + Lesson + "," + Room + "," + NextLesson + "," + NextRoom);
        CurrentLessonText.setText(Lesson);
        CurrentRoomText.setText(Room);
        CurrentRoomText.setTextSize(50);
        LessonStartText.setText(startTime);
        LessonEndText.setText(endTime);
        NextLessonText.setText(NextLesson);
        NextRoomText.setText(NextRoom);
    }

    private void UpdateUiPause(){
        CurrentLessonText.setText("Pause");
        CurrentRoomText.setText("Pause");
        CurrentRoomText.setTextSize(45);
        LessonStartText.setText("");
        LessonEndText.setText("");
        NextLessonText.setText("");
        NextRoomText.setText("");
    }

    private void Extract_Table() { //ToDo pdf tabelle extrahiren
        // Load source PDF document
        String filePath = ""; // ToDo getAssets().open("Plan.pdf").toString();
        com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(filePath);
        com.aspose.pdf.TableAbsorber absorber = new com.aspose.pdf.TableAbsorber();

        // Scan pages
        for (com.aspose.pdf.Page page : pdfDocument.getPages()) {
            absorber.visit(page);
            for (com.aspose.pdf.AbsorbedTable table : absorber.getTableList()) {
                System.out.println("Table");
                // Iterate throught list of rows
                for (com.aspose.pdf.AbsorbedRow row : table.getRowList()) {
                    // Iterate throught list of cell
                    for (com.aspose.pdf.AbsorbedCell cell : row.getCellList()) {
                        for (com.aspose.pdf.TextFragment fragment : cell.getTextFragments()) {
                            StringBuilder sb = new StringBuilder();
                            for (com.aspose.pdf.TextSegment seg : fragment.getSegments())
                                sb.append(seg.getText());
                            System.out.print(sb.toString() + "|");
                        }
                    }
                    System.out.println();
                }
            }
        }
    }


    public class Lesson{
        String Lesson;
        String Room;

        public Lesson(String Lesson,String Room){
            this.Lesson = Lesson;
            this.Room = Room;
        }
    }
}

