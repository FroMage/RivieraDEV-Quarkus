package rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import io.quarkiverse.renarde.Controller;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import model.Language;
import model.Level;
import model.Slot;
import model.Talk;
import model.TalkTheme;
import model.TalkType;
import model.Track;

public class Excel extends Controller {
	@Produces("application/vnd.ms-excel")
	@Path("/excel")
	public byte[] excel() throws IOException {
		try(XSSFWorkbook wb = new XSSFWorkbook()){
			// data
	        List<Date> days = (List)Slot.list(
	                    "select distinct date_trunc('day', startDate) from Slot ORDER BY date_trunc('day', startDate)");
	        List<Track> tracks = Track.listAll();
	        Collections.sort(tracks);
	        List<TalkTheme> themes = TalkTheme.findUsedThemes();
	        List<TalkType> types = TalkType.findUsedTypes();
	        Collections.sort(types);
	        Level[] levels = Level.values();
	        Language[] languages = Language.values();
	        SimpleDateFormat dayFormat = new SimpleDateFormat("dd MMM");
	        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm");
	        
	        // styles
			XSSFCellStyle slotStyle = wb.createCellStyle();
			{
				XSSFFont font = wb.createFont();
				font.setBold(true);
				font.setFontHeightInPoints((short)10);
				slotStyle.setFont(font);
				slotStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			}
			XSSFCellStyle trackStyle = wb.createCellStyle();
			{
				XSSFFont font = wb.createFont();
				font.setBold(true);
				font.setFontHeightInPoints((short)12);
				trackStyle.setFont(font);
				trackStyle.setWrapText(true);
				trackStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			}
			XSSFCellStyle talkStyle = wb.createCellStyle();
			{
				talkStyle.setWrapText(true);
				XSSFFont font = wb.createFont();
				font.setFontHeightInPoints((short)10);
				talkStyle.setFont(font);
				talkStyle.setVerticalAlignment(VerticalAlignment.TOP);
				talkStyle.setBorderLeft(BorderStyle.THIN);
				talkStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
				talkStyle.setBorderBottom(BorderStyle.THIN);
				talkStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
				talkStyle.setBorderRight(BorderStyle.THIN);
				talkStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
				talkStyle.setBorderTop(BorderStyle.THIN);
				talkStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
			}
			XSSFCellStyle allTrackStyle = wb.createCellStyle();
			{
				XSSFFont font = wb.createFont();
				font.setItalic(true);
				font.setFontHeightInPoints((short)10);
				allTrackStyle.setFont(font);
				allTrackStyle.setAlignment(HorizontalAlignment.CENTER);
				allTrackStyle.setVerticalAlignment(VerticalAlignment.CENTER);
				allTrackStyle.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
				allTrackStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			}
	        
	        // schedule
			addRolesSheet(wb);
	        
	        Map<Date, List<Track>> tracksPerDays = new HashMap<Date, List<Track>>();
	        for (Date day : days) {
	            List<Track> tracksPerDay = Talk.findTracksPerDay(day);
	            Collections.sort(tracksPerDay);
	            tracksPerDays.put(day, tracksPerDay);
	            
	            XSSFSheet daySheet = wb.createSheet(dayFormat.format(day));
				daySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, tracksPerDay.size()+3));
	            daySheet.setDefaultColumnWidth(20);
	            daySheet.setColumnWidth(0, 3000);
	            daySheet.setDefaultRowHeight((short) 1200);

				XSSFCellStyle dateJournée = wb.createCellStyle();
		{
			XSSFFont font = wb.createFont();
			font.setBold(true);
			font.setFontHeightInPoints((short)20);
			dateJournée.setFont(font);
			dateJournée.setVerticalAlignment(VerticalAlignment.CENTER);
			dateJournée.setAlignment(HorizontalAlignment.CENTER);
		}
	            
				XSSFRow headerRow = daySheet.createRow(0);
				   
				XSSFCell headerCell = headerRow.createCell(0);
	            headerCell.setCellValue(dayFormat.format(day));
				headerCell.setCellStyle(dateJournée);
				
	            
	            XSSFRow trackRow = daySheet.createRow(3);
	            for (int i = 0; i < tracksPerDay.size(); i++) {
					Track track = tracksPerDay.get(i);
					XSSFCell trackCell = trackRow.createCell(1 + i);
					trackCell.setCellValue(track.title);
					trackCell.setCellStyle(trackStyle);
				}
			XSSFCellStyle AcceuilCellStyle0 = wb.createCellStyle();
		{
			XSSFFont font = wb.createFont();
			font.setBold(true);
			font.setFontHeightInPoints((short)12);
			AcceuilCellStyle0.setFont(font);
			AcceuilCellStyle0.setVerticalAlignment(VerticalAlignment.CENTER);
			AcceuilCellStyle0.setAlignment(HorizontalAlignment.CENTER);
		}
				XSSFCell trackAcceuil = trackRow.createCell(9);
				trackAcceuil.setCellValue("Acceuil badges");
				trackAcceuil.setCellStyle(AcceuilCellStyle0);

				XSSFCell trackInter = trackRow.createCell(10);
				trackInter.setCellValue("""
				Groupe
				d'intervention
				""");
				trackInter.setCellStyle(AcceuilCellStyle0);
				
				XSSFCell trackBackup = trackRow.createCell(11);
				trackBackup.setCellValue("Backup");
				trackBackup.setCellStyle(AcceuilCellStyle0);
				
				
				
				List<Slot> multiPerDay = Slot.findMultiPerDay(day);
	            for (int j = 0; j < multiPerDay.size(); j++) {
	            	XSSFRow slotRow = daySheet.createRow(4+j);
					Slot slot = multiPerDay.get(j);
					Talk allTracksEvent = slot.getAllTracksEvent();
					XSSFCell slotTimeCell = slotRow.createCell(0);
					slotTimeCell.setCellValue(hourFormat.format(slot.startDate) +" - "+ hourFormat.format(slot.endDate));
					
					slotTimeCell.setCellStyle(slotStyle);
					
					if(allTracksEvent != null) {
			            slotRow.setHeight((short) 600);
	                    daySheet.addMergedRegion(new CellRangeAddress(4+j, 4+j, 1, tracksPerDay.size()+3));
						XSSFCell cell = slotRow.createCell(1);
						cell.setCellValue(allTracksEvent.getTitle());
						cell.setCellStyle(allTrackStyle);
					} else {
			            for (int i = 0; i < tracksPerDay.size(); i++) {
							Track track = tracksPerDay.get(i);
							StringBuilder sb = new StringBuilder();
							for (Talk talk : slot.getTalksPerTrack(track)) {
								sb.append(talk.getTitle());
								sb.append("\n");
							}
							XSSFCell cell = slotRow.createCell(1+i);
							cell.setCellValue(sb.toString());
							cell.setCellStyle(talkStyle);
			            }
					}
				}
	        }
	        ByteArrayOutputStream os = new ByteArrayOutputStream();
	        wb.write(os);
	        return os.toByteArray();
		}
	}

	private void addRolesSheet(XSSFWorkbook wb) {
		XSSFSheet rolesSheet = wb.createSheet("Description des roles");
		rolesSheet.setDefaultColumnWidth(20);
	    rolesSheet.setColumnWidth(0, 6500);
        rolesSheet.setDefaultRowHeight((short) 250);
		rolesSheet.setColumnWidth(1, 14500);

		XSSFRow roleRow0 = rolesSheet.createRow(0);
		roleRow0.setHeight((short) 1500);
		XSSFRow roleRow1 = rolesSheet.createRow(1);
		roleRow1.setHeight((short) 1500);
		XSSFRow roleRow2 = rolesSheet.createRow(2);
		roleRow2.setHeight((short) 350);

		XSSFCellStyle roleCellStyle0 = wb.createCellStyle();
		{
			XSSFFont font = wb.createFont();
			font.setBold(true);
			font.setFontHeightInPoints((short)9);
			roleCellStyle0.setFont(font);
			roleCellStyle0.setVerticalAlignment(VerticalAlignment.CENTER);
			roleCellStyle0.setAlignment(HorizontalAlignment.RIGHT);
			roleCellStyle0.setLeftBorderColor(IndexedColors.BLACK.getIndex());
			roleCellStyle0.setBorderBottom(BorderStyle.THIN);
			roleCellStyle0.setBottomBorderColor(IndexedColors.BLACK.getIndex());
			roleCellStyle0.setBorderRight(BorderStyle.THIN);
			roleCellStyle0.setRightBorderColor(IndexedColors.BLACK.getIndex());
			roleCellStyle0.setBorderTop(BorderStyle.THIN);
			roleCellStyle0.setTopBorderColor(IndexedColors.BLACK.getIndex());
		}
		
		XSSFCellStyle roleCellStyle1 = wb.createCellStyle();
		{
			roleCellStyle1.setVerticalAlignment(VerticalAlignment.CENTER);
			roleCellStyle1.setAlignment(HorizontalAlignment.LEFT);
			roleCellStyle1.setLeftBorderColor(IndexedColors.BLACK.getIndex());
			roleCellStyle1.setBorderBottom(BorderStyle.THIN);
			roleCellStyle1.setBottomBorderColor(IndexedColors.BLACK.getIndex());
			roleCellStyle1.setBorderRight(BorderStyle.THIN);
			roleCellStyle1.setRightBorderColor(IndexedColors.BLACK.getIndex());
			roleCellStyle1.setBorderTop(BorderStyle.THIN);
			roleCellStyle1.setTopBorderColor(IndexedColors.BLACK.getIndex());
		}

		XSSFCell roleCell0 = roleRow0.createCell(0);
		roleCell0.setCellValue("Responsables des salles");
		roleCell0.setCellStyle(roleCellStyle0);
		
		XSSFCell roleCell1 = roleRow0.createCell(1);
		roleCell1.setCellValue("""
• S'assure que le speaker est bien installé.
• Donne le go pour commencer quand c'est l'heure.
• Envoie un tweet pendant le talk.
• Signaler quand il reste 10 / 5 / 1 min (vous pouvez utiliser https://countdown.ted.com/).
• Virer le speaker pour laisser le temps aux participant de changer de salle (modifié).""");
		roleCell1.setCellStyle(roleCellStyle1);
		
		XSSFCell roleCell2 = roleRow1.createCell(0);
		roleCell2.setCellValue("Groupe d'intervention");
		roleCell2.setCellStyle(roleCellStyle0);
		
		XSSFCell roleCell3 = roleRow1.createCell(1);
		roleCell3.setCellValue("""
Reste joignable à l'accueil RDEV.
Gérer tout pépin de dernière minutes.
Peut être joint par le reste de l'équipe en cas de souci.
Rotation eau/café.
Gestion des poubelles.""");
		roleCell3.setCellStyle(roleCellStyle1);
		
		XSSFCell roleCell4 = roleRow2.createCell(0);
		roleCell4.setCellValue("Backup");
		roleCell4.setCellStyle(roleCellStyle0);
		
		XSSFCell roleCell5 = roleRow2.createCell(1);
		roleCell5.setCellValue("Libre, mais est contacté en priorité si le \"groupe d'intervention\" est débordé.");
		roleCell5.setCellStyle(roleCellStyle1);
	}

	//
	// Examples
//	
//    private static final String[] days = {
//            "Sunday", "Monday", "Tuesday",
//            "Wednesday", "Thursday", "Friday", "Saturday"};
//
//    private static final String[]  months = {
//            "January", "February", "March","April", "May", "June","July", "August",
//            "September","October", "November", "December"};
//
//	private void demo(XSSFWorkbook wb) {
//        Calendar calendar = LocaleUtil.getLocaleCalendar();
//        int year = calendar.get(Calendar.YEAR);
//
//        Map<String, CellStyle> styles = createStyles(wb);
//
//        for (int month = 0; month < 12; month++) {
//            calendar.set(Calendar.MONTH, month);
//            calendar.set(Calendar.DAY_OF_MONTH, 1);
//            //create a sheet for each month
//            Sheet sheet = wb.createSheet(months[month]);
//
//            //turn off gridlines
//            sheet.setDisplayGridlines(false);
//            sheet.setPrintGridlines(false);
//            sheet.setFitToPage(true);
//            sheet.setHorizontallyCenter(true);
//            PrintSetup printSetup = sheet.getPrintSetup();
//            printSetup.setLandscape(true);
//
//            //the following three statements are required only for HSSF
//            sheet.setAutobreaks(true);
//            printSetup.setFitHeight((short) 1);
//            printSetup.setFitWidth((short) 1);
//
//            //the header row: centered text in 48pt font
//            Row headerRow = sheet.createRow(0);
//            headerRow.setHeightInPoints(80);
//            Cell titleCell = headerRow.createCell(0);
//            titleCell.setCellValue(months[month] + " " + year);
//            titleCell.setCellStyle(styles.get("title"));
//            sheet.addMergedRegion(CellRangeAddress.valueOf("$A$1:$N$1"));
//
//            //header with month titles
//            Row monthRow = sheet.createRow(1);
//            for (int i = 0; i < days.length; i++) {
//                //set column widths, the width is measured in units of 1/256th of a character width
//                sheet.setColumnWidth(i * 2, 5 * 256); //the column is 5 characters wide
//                sheet.setColumnWidth(i * 2 + 1, 13 * 256); //the column is 13 characters wide
//                sheet.addMergedRegion(new CellRangeAddress(1, 1, i * 2, i * 2 + 1));
//                Cell monthCell = monthRow.createCell(i * 2);
//                monthCell.setCellValue(days[i]);
//                monthCell.setCellStyle(styles.get("month"));
//            }
//
//            int cnt = 1, day = 1;
//            int rownum = 2;
//            for (int j = 0; j < 6; j++) {
//                Row row = sheet.createRow(rownum++);
//                row.setHeightInPoints(100);
//                for (int i = 0; i < days.length; i++) {
//                    Cell dayCell_1 = row.createCell(i * 2);
//                    Cell dayCell_2 = row.createCell(i * 2 + 1);
//
//                    int day_of_week = calendar.get(Calendar.DAY_OF_WEEK);
//                    if (cnt >= day_of_week && calendar.get(Calendar.MONTH) == month) {
//                        dayCell_1.setCellValue(day);
//                        calendar.set(Calendar.DAY_OF_MONTH, ++day);
//
//                        if (i == 0 || i == days.length - 1) {
//                            dayCell_1.setCellStyle(styles.get("weekend_left"));
//                            dayCell_2.setCellStyle(styles.get("weekend_right"));
//                        } else {
//                            dayCell_1.setCellStyle(styles.get("workday_left"));
//                            dayCell_2.setCellStyle(styles.get("workday_right"));
//                        }
//                    } else {
//                        dayCell_1.setCellStyle(styles.get("grey_left"));
//                        dayCell_2.setCellStyle(styles.get("grey_right"));
//                    }
//                    cnt++;
//                }
//                if (calendar.get(Calendar.MONTH) > month) break;
//            }
//        }
//	}
//	
//    private static Map<String, CellStyle> createStyles(Workbook wb){
//        Map<String, CellStyle> styles = new HashMap<>();
//
//        short borderColor = IndexedColors.GREY_50_PERCENT.getIndex();
//
//        CellStyle style;
//        Font titleFont = wb.createFont();
//        titleFont.setFontHeightInPoints((short)48);
//        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
//        style = wb.createCellStyle();
//        style.setAlignment(HorizontalAlignment.CENTER);
//        style.setVerticalAlignment(VerticalAlignment.CENTER);
//        style.setFont(titleFont);
//        styles.put("title", style);
//
//        Font monthFont = wb.createFont();
//        monthFont.setFontHeightInPoints((short)12);
//        monthFont.setColor(IndexedColors.WHITE.getIndex());
//        monthFont.setBold(true);
//        style = wb.createCellStyle();
//        style.setAlignment(HorizontalAlignment.CENTER);
//        style.setVerticalAlignment(VerticalAlignment.CENTER);
//        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
//        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//        style.setFont(monthFont);
//        styles.put("month", style);
//
//        Font dayFont = wb.createFont();
//        dayFont.setFontHeightInPoints((short)14);
//        dayFont.setBold(true);
//        style = wb.createCellStyle();
//        style.setAlignment(HorizontalAlignment.LEFT);
//        style.setVerticalAlignment(VerticalAlignment.TOP);
//        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
//        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//        style.setBorderLeft(BorderStyle.THIN);
//        style.setLeftBorderColor(borderColor);
//        style.setBorderBottom(BorderStyle.THIN);
//        style.setBottomBorderColor(borderColor);
//        style.setFont(dayFont);
//        styles.put("weekend_left", style);
//
//        style = wb.createCellStyle();
//        style.setAlignment(HorizontalAlignment.CENTER);
//        style.setVerticalAlignment(VerticalAlignment.TOP);
//        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
//        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//        style.setBorderRight(BorderStyle.THIN);
//        style.setRightBorderColor(borderColor);
//        style.setBorderBottom(BorderStyle.THIN);
//        style.setBottomBorderColor(borderColor);
//        styles.put("weekend_right", style);
//
//        style = wb.createCellStyle();
//        style.setAlignment(HorizontalAlignment.LEFT);
//        style.setVerticalAlignment(VerticalAlignment.TOP);
//        style.setBorderLeft(BorderStyle.THIN);
//        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
//        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//        style.setLeftBorderColor(borderColor);
//        style.setBorderBottom(BorderStyle.THIN);
//        style.setBottomBorderColor(borderColor);
//        style.setFont(dayFont);
//        styles.put("workday_left", style);
//
//        style = wb.createCellStyle();
//        style.setAlignment(HorizontalAlignment.CENTER);
//        style.setVerticalAlignment(VerticalAlignment.TOP);
//        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
//        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//        style.setBorderRight(BorderStyle.THIN);
//        style.setRightBorderColor(borderColor);
//        style.setBorderBottom(BorderStyle.THIN);
//        style.setBottomBorderColor(borderColor);
//        styles.put("workday_right", style);
//
//        style = wb.createCellStyle();
//        style.setBorderLeft(BorderStyle.THIN);
//        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
//        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//        style.setBorderBottom(BorderStyle.THIN);
//        style.setBottomBorderColor(borderColor);
//        styles.put("grey_left", style);
//
//        style = wb.createCellStyle();
//        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
//        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//        style.setBorderRight(BorderStyle.THIN);
//        style.setRightBorderColor(borderColor);
//        style.setBorderBottom(BorderStyle.THIN);
//        style.setBottomBorderColor(borderColor);
//        styles.put("grey_right", style);
//
//        return styles;
//    }

}
