package com.podhod.app;
import org.json.*;
import jxl.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/** Known Sheiko XLS layout and explicit, inspectable flat table format. No macro execution. */
public final class Importer {
    static final String[] LIFTS={"squat","bench","deadlift"};
    public static byte[] read(InputStream in) throws IOException { ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>5*1024*1024)throw new IOException(Lang.t("Файл больше 5 МБ"));out.write(b,0,n);}return out.toByteArray(); }
    public static JSONObject parse(byte[] bytes,String name) throws Exception {
        if(bytes.length>5*1024*1024)throw new IllegalArgumentException(Lang.t("Файл больше 5 МБ"));
        JSONObject p;
        if(bytes.length>2&&(bytes[0]&255)==0xD0&&(bytes[1]&255)==0xCF) p=xls(bytes,name);
        else if(bytes.length>2&&bytes[0]=='P'&&bytes[1]=='K')p=flat(xlsx(bytes),name);
        else {
            String s=new String(bytes,StandardCharsets.UTF_8).replace("\uFEFF","").trim();
            if(s.startsWith("{"))p=Engine.obj(s);
            else p=flat(csv(s),name);
        }
        Engine.validateProgram(p);p.remove("nextDay");p.remove("lastUsed");p.remove("id");p.remove("mine");return p;
    }
    public static List<List<String>> csv(String s) {
        String delimiter=s.split("\\r?\\n",2)[0].contains("\t")?"\t":";";
        List<List<String>> rows=new ArrayList<>();
        for(String line:s.split("\\r?\\n"))if(!line.trim().isEmpty()) rows.add(Arrays.asList(line.split(delimiter,-1)));
        return rows;
    }
    static JSONObject newProgram(String name){JSONObject p=Engine.obj("{\"days\":[],\"maxima\":{},\"restSeconds\":120,\"step\":2.5}");Engine.put(p,"name",name.replaceFirst("\\.[^.]+$",""));return p;}
    static String value(List<String> row,int c){return c<row.size()?row.get(c).trim():"";}
    static double number(String s){return Double.parseDouble(s.replace(',','.'));}
    public static JSONObject flat(List<List<String>> rows,String name) {
        if(rows.size()<2)throw new IllegalArgumentException(Lang.t("Нужна таблица с заголовком и упражнениями"));
        String header=String.join(";",rows.get(0)).toLowerCase(Locale.ROOT).trim();
        if(!header.equals("day;exercise;sets;reps;kg")&&!header.equals("день;упражнение;подходы;повторения;вес"))
            throw new IllegalArgumentException(Lang.t("Поддерживается XLS Шейко или таблица с 5 колонками: день;упражнение;подходы;повторения;вес. Шаблон доступен в библиотеке."));
        JSONObject p=newProgram(name);LinkedHashMap<String,JSONObject> days=new LinkedHashMap<>();
        for(int i=1;i<rows.size();i++) {
            List<String> row=rows.get(i);if(row.size()!=5)throw new IllegalArgumentException(Lang.t("Строка ")+(i+1)+Lang.t(": нужны ровно 5 колонок"));
            String dayName=value(row,0);if(dayName.isEmpty())throw new IllegalArgumentException(Lang.t("Строка ")+(i+1)+Lang.t(": укажи день"));
            JSONObject day=days.get(dayName);
            if(day==null){day=Engine.obj("{\"groups\":[]}");Engine.put(day,"name",dayName);days.put(dayName,day);p.optJSONArray("days").put(day);}
            JSONObject g=new JSONObject();Engine.put(g,"exercise",value(row,1));
            try { Engine.put(g,"sets",Integer.parseInt(value(row,2)));Engine.put(g,"reps",Integer.parseInt(value(row,3)));Engine.put(g,"kg",value(row,4).isEmpty()?-1:number(value(row,4))); }
            catch(Exception e){throw new IllegalArgumentException(Lang.t("Строка ")+(i+1)+Lang.t(": проверь числа"));}
            day.optJSONArray("groups").put(g);
        }
        Engine.validateProgram(p);return p;
    }
    static JSONObject xls(byte[] bytes,String name)throws Exception{
        Workbook w=Workbook.getWorkbook(new ByteArrayInputStream(bytes));
        try {
            Sheet sheet=w.getSheet("План");
            if(sheet==null){List<List<String>> rows=new ArrayList<>();sheet=w.getSheet(0);for(int r=0;r<sheet.getRows();r++){List<String> row=new ArrayList<>();for(int c=0;c<sheet.getColumns();c++)row.add(sheet.getCell(c,r).getContents());if(row.stream().anyMatch(v->!v.trim().isEmpty()))rows.add(row);}return flat(rows,name);}
            if(sheet.getRows()<6||sheet.getColumns()<6||!sheet.getCell(3,4).getContents().equals("Упражнение"))throw new IllegalArgumentException(Lang.t("Неизвестная структура листа План"));
            JSONObject p=newProgram(name);JSONObject maxima=p.optJSONObject("maxima");
            for(int k=0;k<3;k++)Engine.put(maxima,LIFTS[k],((NumberCell)sheet.getCell(1,k)).getValue());
            String week="";JSONObject day=null;int dayNum=0;
            for(int r=5;r<sheet.getRows();r++){
                String wv=sheet.getCell(0,r).getContents();if(!wv.isEmpty()){week=wv;dayNum=0;}
                String exercise=sheet.getCell(3,r).getContents();if(exercise.isEmpty())continue;
                if(sheet.getCell(2,r).getContents().equals("1")){day=Engine.obj("{\"groups\":[]}");Engine.put(day,"name",week+" · День "+(++dayNum));p.optJSONArray("days").put(day);}
                if(day==null)throw new IllegalArgumentException(Lang.t("Не найдено начало дня"));
                for(int c=4;c+1<sheet.getColumns();c+=2){
                    String text=sheet.getCell(c+1,r).getContents().trim();if(text.isEmpty())continue;
                    Matcher m=Pattern.compile("[хx×](\\d+)[хx×](\\d+)").matcher(text);if(!m.matches())throw new IllegalArgumentException(Lang.t("Не распознаны подходы в строке ")+(r+1));
                    JSONObject g=new JSONObject();Engine.put(g,"exercise",exercise);Engine.put(g,"sets",Integer.parseInt(m.group(1)));Engine.put(g,"reps",Integer.parseInt(m.group(2)));
                    Cell cell=sheet.getCell(c,r);Engine.put(g,"kg",cell instanceof NumberCell?((NumberCell)cell).getValue():-1);
                    if(cell instanceof FormulaCell){
                        String f=((FormulaCell)cell).getFormula().replace("$","");
                        Matcher fm=Pattern.compile("(?:ROUND\\()?B([123])\\*([0-9.]+)(?:/2.5,0.0\\)\\*2.5)?").matcher(f);
                        if(!fm.matches())throw new IllegalArgumentException(Lang.t("Неизвестная формула в строке ")+(r+1)+Lang.t(". Импорт остановлен, чтобы не потерять пересчёт весов."));
                        Engine.put(g,"lift",LIFTS[Integer.parseInt(fm.group(1))-1]);Engine.put(g,"factor",Double.parseDouble(fm.group(2)));Engine.put(g,"round",f.startsWith("ROUND")?2.5:0);
                    }
                    day.optJSONArray("groups").put(g);
                }
            }
            return p;
        } finally {w.close();}
    }
    static Document xml(byte[] bytes)throws Exception{
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(false);
        f.setFeature("http://xml.org/sax/features/external-general-entities",false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }
    static List<List<String>> xlsx(byte[] bytes)throws Exception {
        Map<String,byte[]> files=new HashMap<>();int total=0;
        try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry e;while((e=z.getNextEntry())!=null){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=z.read(buf))!=-1){total+=n;if(total>20*1024*1024)throw new IllegalArgumentException(Lang.t("Распакованная таблица слишком большая"));b.write(buf,0,n);}files.put(e.getName(),b.toByteArray());}}
        ArrayList<String> strings=new ArrayList<>();
        if(files.containsKey("xl/sharedStrings.xml")){NodeList list=xml(files.get("xl/sharedStrings.xml")).getElementsByTagName("si");for(int i=0;i<list.getLength();i++)strings.add(list.item(i).getTextContent());}
        if(!files.containsKey("xl/worksheets/sheet1.xml"))throw new IllegalArgumentException(Lang.t("Не найден первый лист XLSX"));
        NodeList list=xml(files.get("xl/worksheets/sheet1.xml")).getElementsByTagName("row");List<List<String>> rows=new ArrayList<>();
        for(int i=0;i<list.getLength();i++){
            List<String> row=new ArrayList<>(Arrays.asList("","","","",""));NodeList cells=((Element)list.item(i)).getElementsByTagName("c");
            for(int j=0;j<cells.getLength();j++){
                Element cell=(Element)cells.item(j);String ref=cell.getAttribute("r");int col=0;for(char ch:ref.toCharArray()){if(!Character.isLetter(ch))break;col=col*26+ch-'A'+1;}
                if(col<1)continue;
                NodeList v=cell.getElementsByTagName("v");String text=v.getLength()>0?v.item(0).getTextContent():"";
                if(cell.getAttribute("t").equals("s"))text=strings.get(Integer.parseInt(text));
                if(cell.getAttribute("t").equals("inlineStr"))text=cell.getTextContent();
                if(col>5){if(!text.trim().isEmpty())throw new IllegalArgumentException(Lang.t("В XLSX должно быть 5 колонок по шаблону"));continue;}
                if(cell.getElementsByTagName("f").getLength()>0)throw new IllegalArgumentException(Lang.t("Для импорта XLSX сохрани значения вместо формул. Процентные формулы поддержаны в XLS Шейко."));
                row.set(col-1,text);
            }
            if(row.stream().anyMatch(s->!s.trim().isEmpty()))rows.add(row);
        }
        return rows;
    }
}
