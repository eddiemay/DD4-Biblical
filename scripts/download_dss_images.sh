#for i in {1..53}
#do
 # curl http://tiles.imj.org.il/columns/isaiah/isaiah"$i".jpg --output src/main/webapp/images/dss/isaiah"$i".jpg
#done

for c in {0..512}
do
  for r in {0..20}
  do
    curl http://tiles.imj.org.il/tiles/isaiah/10_"$c"_"$r".jpg --output src/main/webapp/images/dss/isaiah_10_"$c"_"$r".jpg
  done
done
