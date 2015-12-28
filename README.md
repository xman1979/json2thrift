# json2thrift
Automatically exported from code.google.com/p/json2thrift


Input is json, output is thrift IDL, eg:
test.json:
{
    "id": 1,
    "name": "A green door",
    "price": 12.50,
    "tags": ["home", "green"]
}
After convert:
java -jar json2thrift.jar -f test.json 
you will get:
/* Automatically generated by json2Thrift tool. */

struct MyStruct {
    1: optional list<string> tags,
    2: optional i32 id,
    3: optional double price,
    4: optional string name,
}
Then you can use Apache thrift compiler (http://thrift.apache.org/) to generate code in different languages for data serialization/de-serialization. Especially, with THRIFT-2476 https://issues.apache.org/jira/browse/THRIFT-2476 you can use cpp for simple json processing.
