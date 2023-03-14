import me.xuender.unidecode.Unidecode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;


public class TelBot extends TelegramLongPollingBot {

    private static String searchUrl = "";
    private static ArrayList<String> bookList = new ArrayList<String>();
    private static ArrayList<Integer> pageNumberList = new ArrayList<Integer>();

    @Override
    public String getBotUsername() {
        return "MyBookleBot";
    }

    @Override
    public String getBotToken() {
        return "5887924411:AAF95M77kbRmSb6EvD-jetJZqQNay3NO4rk";
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(update.getMessage().getText());

        SendMessage sndMessage = new SendMessage();

        if (update.hasMessage()) {

            String command = update.getMessage().getText();
            Message rcvMessage = update.getMessage();

            if (rcvMessage.isCommand()) {

                if (command.startsWith(Command.KP)) {

                    searchUrl = "https://novi.kupujemprodajem.com/knjige/pretraga?keywords=";

                    sndMessage.setText("Tražim na Kupujem Prodajem...\n Unesi naziv knjige: ");

                    sndMessage.setChatId(update.getMessage().getChatId());

                    try {
                        execute(sndMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }

                } else if (command.startsWith(Command.LIMUNDO)) {

                    searchUrl = "https://www.limundo.com/pretraga?sSort=aktuelno&sSmer=ASC&txtPretraga=";

                    sndMessage.setText("Tražim na Limundu...\n Unesi naziv knjige: ");

                    sndMessage.setChatId(update.getMessage().getChatId());

                    try {
                        execute(sndMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }

                } else if (command.startsWith(Command.KUPINDO)) {



                } else if (command.startsWith(Command.HELP)) {



                } else if (command.startsWith(Command.BACK)) {



                }
            } else {

                sndMessage.setText("Tražim Knjigu: " + command);  //ODAVDE TRAZIM KNJIGU

                sndMessage.setChatId(update.getMessage().getChatId());

                try {
                    execute(sndMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                // SKREJP METODA KP
                if (searchUrl.contains("https://novi.kupujemprodajem.com")) {

                    try {
                        scrapeKP(searchUrl, command);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (bookList.size()==0) {

                        sndMessage.setText("Nema tražene knjige.");

                        sndMessage.setChatId(update.getMessage().getChatId());

                        try {
                            execute(sndMessage);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }

                    } else {

                        for (int i = 0; i < bookList.size(); i++) {

                            String bookNamePricePage = bookList.get(i);

                            sndMessage.setText(bookNamePricePage);

                            sndMessage.setChatId(update.getMessage().getChatId());

                            try {
                                execute(sndMessage);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    bookList.removeAll(bookList);

                //SKREJP LIMUNDO
                } else if (searchUrl.contains("https://www.limundo.com")) {

                    try {
                        scrapeLimundo(searchUrl, command);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (bookList.size()==0) {

                        sndMessage.setText("Nema tražene knjige.");

                        sndMessage.setChatId(update.getMessage().getChatId());

                        try {
                            execute(sndMessage);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }

                    } else {

                        for (int i = 0; i < bookList.size(); i++) {
                            String bookNamePricePage = bookList.get(i);

                            sndMessage.setText(bookNamePricePage);

                            sndMessage.setChatId(update.getMessage().getChatId());

                            try {
                                execute(sndMessage);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    bookList.removeAll(bookList);

                }
            }
        }
    }

    public static void scrapeKP(String url, String book) throws IOException {

        String changedBookName = Unidecode.decode(book); //SKIDAM KVRZICE SA SLOVA ALI DJ je samo D
        String bookName = changedBookName.replaceAll(" ", "%20");
        int pageNum = 1;
        int maxPage = largestPageKP(url, bookName);

        while (true) {

            String nUrl = url + bookName + "&categoryId=8&page=" + pageNum;
            Document document = Jsoup.parse(new URL(nUrl).openStream(), "UTF-8", "", Parser.xmlParser());
            //System.out.println(document); //DA ISPISE HTML
            Elements bookListing = document.getElementsByClass("AdItem_adHolder__NoNLJ");

            for (Element listing : bookListing) {

                String title = listing.getElementsByClass("AdItem_name__80tI5").text();
                String price = listing.getElementsByClass("AdItem_price__jUgxi").text();
                String link = listing.getElementsByClass("Link_link__J4Qd8").attr("href");
                bookList.add("Knjiga --> " + title + " --> " + price + "\n" + "https://novi.kupujemprodajem.com" + link);

            }

            if (pageNum < maxPage) {
                pageNum++;
            } else {
                //int bookCount = listStorage.bookListSize(); // PROVERAVAM KOLIKO IMA KNJIGA U LISTI
                //System.out.println(listStorage.getBookList()); //ISPISUJE LISTU KNJIGA
                break;

            }
        }
    }

    public static int largestPageKP(String url, String bookName) throws IOException{
        pageNumberList.removeAll(pageNumberList);
        String nUrl = url + bookName + "&categoryId=8";
        Document document = Jsoup.parse(new URL(nUrl).openStream(), "UTF-8", "", Parser.xmlParser());
        //System.out.println(document); //ISPISUJE HTML
        Elements arrows = document.getElementsByClass("Button_children__3mYJw");
        for (Element listArrows : arrows) {
            String text = listArrows.text();
            String value = text.replaceAll("\\D+", "");
            if (value.isEmpty()) {
                continue;
            } else {
                int number = Integer.parseInt(value);
                pageNumberList.add(number);
            }
        }
        int max = pageNumberList.get(0);
        for (int i = 1; i < pageNumberList.size(); i++) {
            if (max < pageNumberList.get(i))
                max = pageNumberList.get(i);
        }
        //System.out.println(max); //ISPISUJE MAX BROJ STRANICE
        return max;
    }

    public static void scrapeLimundo(String url, String book) throws IOException{

        String bookName = book.replaceAll(" ", "+");
        int pageNum = 1;
        int maxPage = largestPageLimundo(url, bookName);

        while (true) {

            String nUrl = url + bookName + "&Okrug=0&Opstina=0&JeKOFP=-1&NapPretKategorijaLimundo=6&IDPodGrupa=0&type=list&page=" + pageNum;
            Document document = Jsoup.parse(new URL(nUrl).openStream(), "UTF-8", "", Parser.xmlParser());
            //System.out.println(document); //DA ISPISE HTML
            Elements bookListing = document.getElementsByClass("row grid-space-0");

            for (Element listing : bookListing) {

                String title = listing.getElementsByClass("content-top").text();
                //String price = listing.getElementsByClass("dark-text").text(); //NE TREBA JER GA TITLE VEC IZBACUJE

                if (title.toLowerCase().contains(book.toLowerCase())) {
                    String link = listing.select("a").attr("href");
                    bookList.add("Knjiga --> " + title + "\n" + link);
                }
            }

            if (pageNum < maxPage) {
                pageNum++;
            } else {

                break;

            }
        }

    }

    public static int largestPageLimundo(String url, String bookName) throws IOException{
        pageNumberList.removeAll(pageNumberList);
        String nUrl = url + bookName + "&Okrug=0&Opstina=0&JeKOFP=-1&NapPretKategorijaLimundo=6&IDPodGrupa=0&type=list";
        Document document = Jsoup.parse(new URL(nUrl).openStream(), "UTF-8", "", Parser.xmlParser());
        //System.out.println(document); //ISPISUJE HTML
        Elements arrows = document.getElementsByClass("page-link");
        for (Element listArrows : arrows) {
            String text = listArrows.text();
            String value = text.replaceAll("\\D+", "");
            if (value.isEmpty()) {
                continue;
            } else {
                int number = Integer.parseInt(value);
                pageNumberList.add(number);
            }
        }
        //System.out.println(pageNumberList); //PRINT LISTE
        int max = pageNumberList.get(0);
        for (int i = 1; i < pageNumberList.size()-1; i++) {
            if (max < pageNumberList.get(i))
                max = pageNumberList.get(i);
        }
        //System.out.println(max); //ISPISUJE MAX BROJ STRANICE
        return max;
    }
}