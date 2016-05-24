import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Created by ferranferri on 23/4/16
 */
public class ProfanityWordsTests {

    private static final String BASE_URL = "www.purgomalum.com";

    @DataProvider(name = "CorrectStrings")
    public static Object[][] CorrectStrings() {
        return new Object[][]
                {
                        {"xml", "This is a test sentence", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><PurgoMalum xmlns=\"http://www.purgomalum.com\"><result>This is a test sentence</result></PurgoMalum>"},
                        {"json", "Esta frase también tiene que ser correcta", "{\"result\":\"Esta frase también tiene que ser correcta\"}"},
                        {"plain", "En aquesta frase tampoc n'hi ha cap paraula incorrecta", "En aquesta frase tampoc n'hi ha cap paraula incorrecta"},
                        {"plain", "En aquesta frase tampoc n'hi ha cap paraula fuck incorrecta", "En aquesta frase tampoc n'hi ha cap paraula **** incorrecta"}
                };
    }

    @DataProvider
    public static Object[][] ProfanityCheckValues() {
        return new Object[][]
                {
                        {"This is a correct sentence", false},
                        {"Fucking sentence", true},
                        {"A hidden sentencefukker", true},
                        {"-$$$fukker345345", true},
                        {"fu_k_k_er", true},
                        {"$ucker!!! You are an @sshole!", true}
                };
    }

    @DataProvider
    public static Object[][] AddWordsToProfanityList() {
        return new Object[][]
                {
                        //{"cançalada", "canguro,pedo,cançalada,perro"},
                        //{"maricón", "pepito,mariquita,maricón"},
                        {"canguro", "canguro"},
                        {"canguro anguila", "pedo, caca, anguila"},
                        {"c:a_n$$guro", "canguro"}
                };
    }

    @DataProvider
    public static Object[][] ReplaceBlackListedWords() {
        return new Object[][]
                {
                        {"This sentences contains word bitch", "This sentences contains word *****"},
                        {"You are a fu_ck_er", "You are a ******"},
                        {"Fucking assholes!!!!", "******* ********!!!!"}
                };
    }

    @DataProvider
    public static Object[][] ReplaceBlacklistedWordsWithCustom() {
        return new Object[][]
                {
                        {"This fucking term", "This [CENSORED] term", "[CENSORED]"},
                        {"This fucking term", "This [BANNED] term", "[BANNED]"},
                        {"This fucking term", "This palabra_prohibida term", "palabra_prohibida"}
                };
    }

    @DataProvider
    public static Object[][] datapro() {
        return new Object[0][];
    }

    private String makeRequestWithCensoredTermAndProcess(String responseType, String textToCheck, String censoredTerm) throws Exception {
        return makeRequestURIAndProcess(responseType, "text=" + textToCheck + "&fill_text=" + censoredTerm);
    }

    private String makeRequestAndProcess(String responseType, String textToCheck, String wordsToAdd) throws Exception {
        return makeRequestURIAndProcess(responseType, "text=" + textToCheck + "&add=" + wordsToAdd);
    }

    private String makeRequestAndProcess(String responseType, String textToCheck) throws Exception {
        textToCheck = textToCheck.replace(" ", "%20");
        return makeRequestURIAndProcess(responseType, "text=" + textToCheck);
    }

    private String makeRequestURIAndProcess(String responseType, String command) throws Exception {
        URI uri = new URI(
                "http",
                BASE_URL,
                "/service/" + responseType,
                command,
                null
        );
        String request = uri.toASCIIString();
        URL url = new URL(request);
        return makeRequestURLAndProcess(url);
    }

    private String makeRequestURLAndProcess(URL url) throws Exception{


        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        Assert.assertEquals(connection.getResponseCode(), 200);

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (connection.getInputStream())));

        String response = br.readLine();
        connection.disconnect();

        return URLDecoder.decode(response, "UTF-8");
    }

    @Test(dataProvider = "CorrectStrings")
    public void testRequestTypes(String responseType, String textToCheck, String expectedResult) throws Exception {
        String result = makeRequestAndProcess(responseType, textToCheck);
        Assert.assertEquals(result, expectedResult);
    }

    @Test(dataProvider = "ProfanityCheckValues")
    public void testContainsProfanity(String textToCheck, boolean expectedResult) throws Exception {
        String result = makeRequestAndProcess("containsprofanity", textToCheck);
        Assert.assertEquals(Boolean.parseBoolean(result.toLowerCase()), expectedResult);
    }

    @Test(dataProvider = "AddWordsToProfanityList")
    public void testAddWordsToProfanityList(String textToCheck, String wordsToAdd) throws Exception {
        String result = makeRequestAndProcess("containsprofanity", textToCheck, wordsToAdd);
        Assert.assertTrue(Boolean.parseBoolean(result.toLowerCase()));
    }

    @Test(dataProvider = "ReplaceBlackListedWords")
    public void Jira_2344_testReplaceBlacklistedWords(String textToCheck, String expectedResult) throws Exception {
        String result = makeRequestAndProcess("plain", textToCheck);
        Assert.assertEquals(result, expectedResult);
    }

    @Test(dataProvider = "ReplaceBlacklistedWordsWithCustom")
    public void testReplacBlackListedWordWithCustomWord(String textToCheck, String expectedText, String wordToFill) throws Exception {
        String result = makeRequestWithCensoredTermAndProcess("plain", textToCheck, wordToFill);
        Assert.assertEquals(result, expectedText);
    }

    @Test(dataProvider = "datapro")
    public void testName() throws Exception {

    }
}
