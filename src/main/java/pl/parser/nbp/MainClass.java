package pl.parser.nbp;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
/*
    Klasa startowa. W pakiecie pl.parser.nbp znajduje się takze klasa 'TabelaKursowTMP' ktora nie jest uzywana.
    Jest to kopia klasy generowanej automatycznie na podstawie pliku 'schema.xsd' przy użyciu narzędzia "jaxb2-maven-plugin".
    Zamiescilem ją "na wszelki wypadek" - miałem u siebie problemy z gnerowaniem sourców w jednej z wersji Eclipse.
    Gdyby były jakieś problemy przy generowaniu wystarczy zmienic nazwę klasy TabelaKursowTMP na TabelaKursow (oraz zmienić importy...)
    
 */
public class MainClass {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx;
        if (args.length != 3) {
            System.out.println("Za mało argumentów.");
        } else {
            ctx = new AnnotationConfigApplicationContext(AppConfig.class);
            CurrencyCalculator currencyCalculator = (CurrencyCalculator) ctx.getBean("currencyCalculator");
            try {
                currencyCalculator.calculateResults(args[0], args[1], args[2]);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.close();
            }
        }
    }
}


