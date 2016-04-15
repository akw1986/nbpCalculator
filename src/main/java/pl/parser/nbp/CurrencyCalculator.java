package pl.parser.nbp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import javax.xml.bind.JAXBException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CurrencyCalculator {

    @Autowired
    private NBPDataService NBPDataService;

    public void calculateResults(String currencyName, String startDateAsString, String endDateAsString) throws DatesInWrongOrderException, JAXBException, ParseException {
        List<TabelaKursow> priceTables = NBPDataService.getPriceTables(startDateAsString, endDateAsString);

        List<Double> currencyBuyPrice = new ArrayList<Double>();
        List<Double> currencySellPrice = new ArrayList<Double>();

        for (TabelaKursow tk : priceTables) {
            for (TabelaKursow.Pozycja pozycja : tk.getPozycja()) {
                if (pozycja.getKodWaluty().equals(currencyName)) {
                    String buyPrice = pozycja.getKursKupna().replaceAll(",", ".");
                    currencyBuyPrice.add(Double.valueOf(buyPrice));

                    String sellPrice = pozycja.getKursSprzedazy().replaceAll(",", ".");
                    currencySellPrice.add(Double.valueOf(sellPrice));
                }
            }
        }
        DecimalFormat df = new DecimalFormat("0.0000");

        if (currencyBuyPrice.isEmpty()) {
            System.out.println("Brak danych do policzenia średniej ceny kupna waluty " + currencyName);
        } else {
            System.out.println(df.format(calculateMeanValue(currencyBuyPrice)));
        }

        if (currencySellPrice.isEmpty()) {
            System.out.println("Brak danych do policzenia odchylenia standardowego ceny sprzedaży waluty " + currencyName);
        } else {
            System.out.println(df.format(calculateMeanDeviation(currencySellPrice)));
        }
    }

    private Double calculateMeanValue(List<Double> numbers) {
        if (numbers.size() > 0) {
            Double sum = 0d;
            for (Double number : numbers) {
                sum += number;
            }
            return sum / numbers.size();
        } else {
            return 0d;
        }
    }

    private Double calculateMeanDeviation(List<Double> numbers) {
        if (numbers.size() > 0) {
            Double sum = 0d;
            Double meanValue = calculateMeanValue(numbers);
            for (Double number : numbers) {
                Double diff = number - meanValue;
                sum += diff * diff;
            }
            return Math.sqrt(sum / numbers.size());
        } else {
            return 0d;
        }
    }
}
