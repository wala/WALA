// Example Code

public class example {
    public String convert_to_json(T dataStructure) throws IOException {
        Gson gson = new Gson();
        System.out.println("gson");
        String json_format = gson.toJson(dataStructure);
        FileWriter writer = new FileWriter("output2.json");
        writer.write(json_format);
        writer.close();
        return json_format;
    }
    public String main() {
        String out = convert_to_json("null");
        return out;
    }

}

// example output

{
    {
        "Class_Name": "example",
        "Functions": ["convert_to_json", "main"]
    },
    {
        "Class_Name": "convert_to_json",
        "Functions": ["Gson", "toJson", "FileWriter", "write", "close"]
    },
    {
        "Class_Name": "main",
        "Functions": ["convert_to_json"]
    }
}
