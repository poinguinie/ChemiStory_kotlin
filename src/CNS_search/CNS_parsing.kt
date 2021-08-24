import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class Molecular(var _root: JSONObject) {
    var root = _root
    var name: String = ""
    var name_noTag: String = ""
    var rn: String = ""
    var image: String = ""
    var molecularMass: String = ""
    var boiling_point: String = ""
    var melting_point: String = ""
    var density: String = ""
    var density_noTag: String = ""

    init {
        this.name = this.root.getString("name")
        this.name_noTag = this.name.replace("<[^>]*>".toRegex(),"")
        this.rn = this.root.getString("rn")
        this.image = this.root.getString("image").replace("\\","")
        this.molecularMass = this.root.getString("molecularMass")

        var experimentalProperties  = this.root.getJSONArray("experimentalProperties")
        for (i in 0 until experimentalProperties.length()) {
            if (i == 0) {
                this.boiling_point = JSONObject(experimentalProperties[0].toString())["property"].toString()
            }
            else if (i == 1) {
                this.melting_point = JSONObject(experimentalProperties[1].toString())["property"].toString()
            }
            else if (i == 2) {
                this.density = JSONObject(experimentalProperties[2].toString())["property"].toString()
                this.density_noTag = this.density.replace("<[^>]*>".toRegex(),"")
            }
        }
    }
}

fun translateFromKorToEng(kor: String): JSONObject {
    var clientID = ""
    var clientSecret = ""

    try {

        val text = URLEncoder.encode(kor, "UTF-8") //넘어온게 배열이었어?
        val apiURL = "https://openapi.naver.com/v1/papago/n2mt"
        val url = URL(apiURL)
        val con = url.openConnection() as HttpURLConnection
        con.setRequestMethod("POST");
        con.setRequestProperty("X-Naver-Client-Id", clientID);
        con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
        // post request
        val postParams = "source=ko&target=en&text=" + text
        con.doOutput = true
        val wr = DataOutputStream(con.outputStream)
        wr.writeBytes(postParams)
        wr.flush()
        wr.close()
        val responseCode = con.responseCode
        val br: BufferedReader
        if (responseCode === 200) { // 정상 호출
            br = BufferedReader(InputStreamReader(con.inputStream))
        } else {                    // 에러 발생
            br = BufferedReader(InputStreamReader(con.errorStream))
        }
        var inputLine: String? = null
        val response = StringBuffer()
        while ({inputLine = br.readLine(); inputLine}() != null) {
            response.append(inputLine)
        }
        br.close()
        return JSONObject(response.toString())

    } catch (e: Exception) {
        println(e)
    }

    return JSONObject()
}

fun isKorean(s: String): Boolean {
    var i = 0
    while (i < s.length) {
        val c = s.codePointAt(i)
        if (c in 0xAC00..0xD800)
            return true
        i += Character.charCount(c)
    }
    return false
}


fun returnJsonFromCAS(type: String, query_type: String, query_value: String): JSONObject {
    var site: String? = null
    var value = URLEncoder.encode(query_value, "UTF-8")
    if (type == "search") {
        site = "https://commonchemistry.cas.org/api/"+type+"?"+query_type+"="+value + "&size=5&offset=0"
    }
    else {
        site = "https://commonchemistry.cas.org/api/"+type+"?"+query_type+"="+value
    }

    var url = URL(site)
    var conn = url.openConnection()
    var input = conn.getInputStream()
    var isr = InputStreamReader(input)
    // br: 라인 단위로 데이터를 읽어오기 위해서 만듦
    var br = BufferedReader(isr)

    // Json 문서는 일단 문자열로 데이터를 모두 읽어온 후, Json에 관련된 객체를 만들어서 데이터를 가져옴
    var str: String? = null
    var buf = StringBuffer()

    do{
        str = br.readLine()

        if(str!=null){
            buf.append(str)
        }
    }while (str!=null)

    // 전체가 객체로 묶여있기 때문에 객체형태로 가져옴
    return JSONObject(buf.toString())
}

fun main() {

    print("Enter the Molecular : ")
    var input: String? = ""
    input= readLine()
    var searchRoot: JSONObject = JSONObject()
    if (input is String && isKorean(input)) {
        var translateJSON = translateFromKorToEng(input)
        var message = JSONObject(translateJSON["message"].toString())
        var result = JSONObject(message["result"].toString())
        var translatedText = result["translatedText"].toString()
        searchRoot = returnJsonFromCAS("search","q",translatedText)
    }
    else if (input is String){
        searchRoot = returnJsonFromCAS("search","q",input)
    }
    else {
        println("Input is Not String")
    }

    if (searchRoot["count"].toString() == "0" || searchRoot == null) {
        println("can't find")
    }
    else {
        var searchResult = searchRoot.getJSONArray("results")

        var searchRN = searchResult.getJSONObject(0).getString("rn")

        var detailRoot = returnJsonFromCAS("detail","cas_rn",searchRN)



        var m = Molecular(detailRoot)

        println("Name : " + m.name_noTag)
        println("RN : " + m.rn)
        println("Image : " + m.image)
        println("MolecularMass : " + m.molecularMass + "g/mol")
        println("Boiling_point : " + m.boiling_point)
        println("Melting_point : " + m.melting_point)
        println("Density : " + m.density_noTag)
    }
}


