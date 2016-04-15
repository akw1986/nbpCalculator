package pl.parser.nbp;

import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Algorytm poszukiwania danych na serwerze wygląda następująco:
 * 1. Znaleźć wszystkie daty (w formacie NBP) znajdujące się pomiędzy datą początkową a datą końcową okresu.
 * 2. Znaleźć wszystkie "katalogi" z opisem istniejących notować, tzn. wybrać pliki dir*.txt odpowiednie dla danego okresu.
 * 3. Z danych z plików z punktu 2. utworzyć listę wszystkich nazw plików znajdujących się w tych plikach.
 * 4. Dla każdej daty z punktu 1. sprawdzić czy istnieje plik pasujący do niej. Jeśli taki plik istnieje - wybrać go do odczytania.
 * 5. Po kroku czwartym dysponujemy listę nazw plików które zawierają dane z notowań które wystąpiły w szukanym okresie. Dzięki temu odczytujemy
 *    tylko te pliki z danymi notowań które są nam niezbędne...
 *
 *  To wszystko można było uzyskać "łatwiej" pod względem programistycznym przy znacznie większym ruchu sieciowym / ilości parsowań dokumentów XML:
 *  1. na podstawie daty początkowej i końcowej wyznaczamy pliki dir*.txt które zawieraja dane o nazwach plików z danymi notowań.
 *  2. odpytujemy serwer o kolejne pliki o nazwach pochodzących z punktu 1. (czyli wszystkie dla danego roku !)
 *  3. parsujemy każdy pobrany plik na klasę TabelaNotowan
 *  4. sprawdzamy datę która się kryje pod polem 'dataNotowan' dla kazdego dokumentu z punktu 3. i na tej podstawie decydujemy czy notowanie liczy się do
 *     średniej / odchylenia standardowego czy nie.
 *
 */
@Service
public class NBPDataService {

    private static final String INPUT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String NBP_DATE_FORMAT = "yyMMdd";

    private static final DateFormat inputDateFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
    private static final DateFormat nbpDateFormat = new SimpleDateFormat(NBP_DATE_FORMAT);
    private static final String HTTP_WWW_NBP_PL_KURSY_XML = "http://www.nbp.pl/kursy/xml/";

    /**
     * @param startDateAsString data początkowa liczonego okresu
     * @param endDateAsString   data końcowa liczonego okresu
     * @return lista obiektów reprezentujących pobrane dane z serwisu NBP
     * @throws DatesInWrongOrderException
     * @throws JAXBException
     * @throws ParseException
     */
    public List<TabelaKursow> getPriceTables(String startDateAsString, String endDateAsString) throws DatesInWrongOrderException, JAXBException, ParseException {
        Date startDate = inputDateFormat.parse(startDateAsString);
        Date endDate = inputDateFormat.parse(endDateAsString);
        if (startDate.after(endDate)) {
            throw new DatesInWrongOrderException(startDate, endDate);
        }
        List<TabelaKursow> priceTables = new ArrayList<TabelaKursow>();
        JAXBContext jc = JAXBContext.newInstance(TabelaKursow.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();

        List<String> xmlFileNamesToRead = getXMLFileNames(startDate, endDate);
        for (String xmlFileName : xmlFileNamesToRead) {
            InputStream xmlFileAsInputStream = readXMLFile(xmlFileName);
            TabelaKursow tk = (TabelaKursow) unmarshaller.unmarshal(xmlFileAsInputStream);
            priceTables.add(tk);
        }
        return priceTables;
    }

    /**
     *
     * @param startDate początek okresu
     * @param endDate koniec okresu
     * @return lista nazw plików xml które zawierają dane dla zadanego okresu czasu.
     */
    private List<String> getXMLFileNames(Date startDate, Date endDate) {

        List<String> xmlFileNames = new ArrayList<String>();
        Set<String> indexFileNamesToRead = getIndexFileNamesToRead(startDate, endDate);
        List<String> daysBetweenDates = getDaysBetweenDates(startDate, endDate);
        Set<String> fileNamesFromIndexFiles = getFileNamesFromIndexFiles(indexFileNamesToRead);

        //Szukamy dla których dat z podanego okresu znajdziemy pliki na serwerze z danymi...
        for (String date : daysBetweenDates) {
            for (String fileName : fileNamesFromIndexFiles) {
                if (fileName.endsWith(date)) {
                    xmlFileNames.add(fileName + ".xml");
                }
            }
        }
        return xmlFileNames;
    }

    /**
     *
     * @param indexFileNamesToRead nazwa pliku z indeksem (dir.txt, dir2015.txt, dir
     * @return
     */
    private Set<String> getFileNamesFromIndexFiles(Set<String> indexFileNamesToRead) {
        Set<String> fileNames = new HashSet<String>();
        for (String indexFileName : indexFileNamesToRead) {
            fileNames.addAll(readFile(indexFileName));
        }
        return fileNames;
    }

    private Set<String> readFile(String fileName) {
        Set<String> fileNames = new HashSet<String>();
        try {
            URL url = new URL(HTTP_WWW_NBP_PL_KURSY_XML + fileName);
            InputStream is = url.openStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String read;

            while ((read = br.readLine()) != null) {
                //interesują nas pliki tylko o nazwie zaczynajacej sie od "c"
                if (read.startsWith("c")) {
                    fileNames.add(read);
                }
            }

            br.close();
        } catch (Exception e) {

        }
        return fileNames;
    }

    private InputStream readXMLFile(String fileName) {
        InputStream is = null;
        try {
            URL url = new URL(HTTP_WWW_NBP_PL_KURSY_XML + fileName);
            is = url.openStream();
        } catch (Exception e) {

        }
        return is;
    }

    /**
     *
     * @param startdate początek okresu
     * @param enddate koniec okresu
     * @return zwraca wszystkie daty z danego okresu w formacie używanym przez serwis NBP
     */
    private static List<String> getDaysBetweenDates(Date startdate, Date enddate) {
        List<String> dates = new ArrayList<String>();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(startdate);

        while (calendar.getTime().before(enddate)) {
            Date result = calendar.getTime();
            dates.add(nbpDateFormat.format(result));
            calendar.add(Calendar.DATE, 1);
        }
        dates.add(nbpDateFormat.format(calendar.getTime()));
        return dates;
    }

    /**
     *
     * @param startDate
     * @param endDate
     * @return zwraca listę indeksów z nazwami plików do przeczytania dla danego okresu. Przykładowo:
     * okres: od 02-02-2013 do 01-05-2013 - czytamy plik dir2013.txt,
     * okres: od 02-02-2016 do 01-05-2016 - czytamy plik dir.txt (jeśli 2016 wciąż jest bieżącym rokiem...)
     * okres: od 02-02-2013 do 01-05-2016 - czytamy pliki dir2013.txt, dir2014.txt, dir2015.txt oraz dir.txt
     *
     */
    private Set<String> getIndexFileNamesToRead(Date startDate, Date endDate) {
        Set<String> yearsToReadAsString = new HashSet<String>();
        Set<String> fileNamesToRead = new HashSet<String>();
        Calendar calendar = new GregorianCalendar();
        Integer actualYear = calendar.get(Calendar.YEAR);
        calendar.setTime(startDate);

        while (calendar.getTime().before(endDate)) {
            Integer i = calendar.get(Calendar.YEAR);
            yearsToReadAsString.add(i.toString());
            calendar.add(Calendar.YEAR, 1);
        }

        for (String year : yearsToReadAsString) {
            if (year.equals(actualYear.toString())) {
                fileNamesToRead.add("dir.txt");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("dir").append(year).append(".txt");
                fileNamesToRead.add(sb.toString());
            }
        }
        return fileNamesToRead;
    }

}
