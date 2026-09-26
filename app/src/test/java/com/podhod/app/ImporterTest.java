package com.podhod.app;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
public class ImporterTest {
    @Test public void originalXlsPreservesEveryWeightFormula()throws Exception{
        byte[] bytes=Importer.read(getClass().getResourceAsStream("/sheiko.xls"));
        JSONObject p=Importer.parse(bytes,"Шейко.xls");assertEquals(16,p.getJSONArray("days").length());
        int groups=0,sets=0,formulas=0;
        for(int i=0;i<16;i++){
            JSONArray a=p.getJSONArray("days").getJSONObject(i).getJSONArray("groups");groups+=a.length();
            for(int j=0;j<a.length();j++){JSONObject g=a.getJSONObject(j);sets+=g.getInt("sets");if(g.has("factor")){formulas++;assertEquals(g.getDouble("kg"),Engine.weight(g,p.getJSONObject("maxima"),2.5),.00001);}}
        }
        assertEquals(217,groups);assertEquals(565,sets);assertEquals(178,formulas);
    }
    byte[] xlsx(boolean formula)throws Exception{
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        try(ZipOutputStream z=new ZipOutputStream(b)){
            z.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            String xml="<worksheet><sheetData><row>";
            String[] headers={"day","exercise","sets","reps","kg"};
            for(int i=0;i<5;i++)xml+="<c r='"+(char)('A'+i)+"1' t='inlineStr'><is><t>"+headers[i]+"</t></is></c>";
            xml+="</row><row><c r='A2' t='inlineStr'><is><t>Day 1</t></is></c><c r='B2' t='inlineStr'><is><t>Squat</t></is></c><c r='C2'><v>3</v></c><c r='D2'><v>5</v></c><c r='E2'>"+(formula?"<f>100*0.6</f>":"")+"<v>60</v></c></row></sheetData></worksheet>";
            z.write(xml.getBytes(StandardCharsets.UTF_8));z.closeEntry();
        }return b.toByteArray();
    }
    @Test public void flatXlsxImportsValues()throws Exception{JSONObject p=Importer.parse(xlsx(false),"Plan.xlsx");assertEquals(60,Engine.start(p,0).getJSONArray("sets").getJSONObject(0).getDouble("kg"),0);}
    @Test public void xlsxRejectsFormulaRatherThanUsingStaleCache()throws Exception{assertThrows(IllegalArgumentException.class,()->Importer.parse(xlsx(true),"Plan.xlsx"));}
}
