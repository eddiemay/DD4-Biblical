echo "const DSS_ISA_BY_COLUMN = [" > src/main/js/DSS_COORDS.js
for c in {1..54}
do
  curl --request GET --header "content-type:application/json" http://dss.collections.imj.org.il/api/get_verse_by_column?col=$c >> src/main/js/DSS_COORDS.js
  echo "," >> src/main/js/DSS_COORDS.js
done
echo = "]"