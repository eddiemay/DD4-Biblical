echo "Echo"
curl --request POST --header "content-type:application/json" --data '{"message":"hello world"}' https://dd4-biblical.appspot.com/_api/echo/v1/echo?n=5
curl --request GET --header "content-type:application/json" https://dd4-biblical.appspot.com/_api/echo/v1/hello?n=3

echo "\n\nBase Services"
curl --request GET --header "content-type:application/json" https://dd4-biblical.appspot.com/_api/biblicalEvents/v1/month/1
curl --request GET --header "content-type:application/json" https://dd4-biblical.appspot.com/_api/calendarRules/v1/_
curl --request GET --header "content-type:application/json" https://dd4-biblical.appspot.com/_api/calendarValidator/v1/validate?type=Enoch

echo "\n\nScripture Service"
curl --request GET --header "content-type:application/json" https://dd4-biblical.appspot.com/_api/scriptures/v1/books
curl --request GET --header "content-type:application/json" https://dd4-biblical.appspot.com/_api/scriptures/v1/scriptures?reference=Genesis%201:1
curl --request GET --header "content-type:application/json" https://dd4-biblical.appspot.com/_api/scriptures/v1/search?searchText=Gen%20moon
# curl --request GET --header "content-type:application/json" https://dd4-biblical.appspot.com/_api/scriptures/v1/html?reference=Genesis%201:1
# curl --request POST --header "content-type:application/json" --data '{"html":"Elohim created all <div class=\"scripture\">Genesis 1:1</div"}' https://dd4-biblical.appspot.com/_api/scriptures/v1/expand

echo "\n\nMulipulation"
# curl --request GET --header "content-type:application/json" https://dd4-biblical.appspot.com/_api/scriptures/v1/searchAndDelete?searchText=book%3DEcclesiasticus&idToken=486903173