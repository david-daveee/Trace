import jxl.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public class DumpWorkbook {
  public static void main(String[] args) throws Exception {
    Workbook w=Workbook.getWorkbook(new File(args[0]));
    try(PrintWriter out=new PrintWriter(new OutputStreamWriter(new FileOutputStream(args[1]),StandardCharsets.UTF_8))) {
      for(Sheet s:w.getSheets()) for(int r=0;r<s.getRows();r++) for(int c=0;c<s.getColumns();c++) {
        Cell x=s.getCell(c,r);
        if(!x.getContents().isEmpty()) out.println(s.getName()+"\t"+(r+1)+"\t"+(c+1)+"\t"+x.getContents().replace('\t',' ').replace('\n',' ')+"\t"+(x instanceof FormulaCell ? ((FormulaCell)x).getFormula():""));
      }
    }
    w.close();
  }
}
