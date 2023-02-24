classpath="-cp target/DD4-Biblical-1.0/WEB-INF/classes/:target/DD4-Biblical-1.0/WEB-INF/lib/*"
java $classpath com.digitald4.biblical.tools.ScriptureDeleter "$1" 145409502

# curl --request POST --header "content-type:application/json" --data '{"message":"hello world"}' https://dd4-biblical.appspot.com/_api/echo/v1/echo?n=5
# curl --request POST --header "content-type:application/json" --data '{"items":["hello_world","hw"]}' https://dd4-biblical.appspot.com/_api/scriptures/v1/batchDelete