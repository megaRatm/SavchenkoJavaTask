import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

public class main{

    public static void main(String[] args) throws Exception {
        HttpURLConnection conn = null;
        String INN, KPP;
        String response = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter INN: ");
        INN = reader.readLine();
        System.out.print("Enter KPP: ");
        KPP = reader.readLine();

        try {
            /*
            Создание POST-запроса
             */
            URL url = new URL("http://npchk.nalog.ru/ajax.html");
            Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put("inn", INN);
            params.put("kpp", KPP);
            params.put("dt", new SimpleDateFormat("MM.dd.yyyy").format(Calendar.getInstance().getTime()));

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);

            /*
            Чтение и анализ ответа сервера
             */

            Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

            for (int c; (c = in.read()) >= 0;)
                response += (char) c;


            switch (Integer.parseInt(response.substring(1, response.length()-1))){
                case 0:
                    System.out.println("Налогоплательщик зарегистрирован в ЕГРН и имел статус действующего в указанную дату");
                    break;
                case 1:
                    System.out.println("Налогоплательщик зарегистрирован в ЕГРН, но не имел статус действующего в указанную\n" +
                            "дату");
                    break;
                case 2:
                    System.out.println("Налогоплательщик зарегистрирован в ЕГРН");
                    break;
                case 3:
                    System.out.println("Налогоплательщик с указанным ИНН зарегистрирован в ЕГРН, КПП не соответствует ИНН\n" +
                            "или не указан");
                    break;
                case 4:
                    System.out.println("Налогоплательщик с указанным ИНН не зарегистрирован в ЕГРН");
                    break;
                default:
                    System.out.println("request error");
                    break;
            }


        }catch (Exception cause){
            if(conn.getResponseCode() == 400) System.out.println("ERROR REQUEST:");
            else cause.printStackTrace();
        }finally {
            /*
            В случае, если запрос составлен неверно, считываем и парсим JSON-объект ошибки
             */
            if(conn.getResponseCode() == 400) {
                InputStream errorStream = conn.getErrorStream();
                String responseErrorJson = "";
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(errorStream));

                while ((line = br.readLine()) != null) {
                    responseErrorJson += line;
                }

                JsonObject jsonObject = new JsonParser().parse(responseErrorJson).getAsJsonObject();
                if(jsonObject.get("ERRORS").getAsJsonObject().get("inn") != null)
                    System.out.println(jsonObject.get("ERRORS").getAsJsonObject().get("inn").getAsString());
                if(jsonObject.get("ERRORS").getAsJsonObject().get("kpp") != null)
                    System.out.println(jsonObject.get("ERRORS").getAsJsonObject().get("kpp").getAsString());

                if(conn != null) conn.disconnect();
            }

        }
    }
}

