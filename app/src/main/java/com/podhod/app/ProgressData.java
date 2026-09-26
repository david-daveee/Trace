package com.podhod.app;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.json.*;
/** Activity comes only from saved sessions, grouped in the phone's local time zone. */
public final class ProgressData {
    final TreeMap<LocalDate,ArrayList<JSONObject>> days=new TreeMap<>();
    final LocalDate today;
    int sessions,sets,last30,activeDays,weeklyStreak;
    ProgressData(JSONArray history,LocalDate today,ZoneId zone){
        this.today=today;
        for(int i=0;i<history.length();i++){
            JSONObject s=history.optJSONObject(i);if(s==null||s.optLong("ended")<=0)continue;
            LocalDate day=Instant.ofEpochMilli(s.optLong("ended")).atZone(zone).toLocalDate();if(day.isAfter(today))continue;
            days.computeIfAbsent(day,k->new ArrayList<>()).add(s);sessions++;sets+=Engine.doneCount(s);
            if(!day.isBefore(today.minusDays(29)))last30++;
        }
        activeDays=days.size();HashSet<LocalDate> weeks=new HashSet<>();for(LocalDate d:days.keySet())weeks.add(d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        LocalDate week=today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));if(!weeks.contains(week))week=week.minusWeeks(1);
        while(weeks.contains(week)){weeklyStreak++;week=week.minusWeeks(1);}
    }
    int count(LocalDate date){return days.containsKey(date)?days.get(date).size():0;}
    int sets(LocalDate date){int n=0;if(days.containsKey(date))for(JSONObject s:days.get(date))n+=Engine.doneCount(s);return n;}
    int level(LocalDate date){int n=sets(date);return n==0?0:n<=5?1:n<=10?2:n<=20?3:4;}
}
