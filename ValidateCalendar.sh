date=$1
classpath="-cp target/DD4-Biblical-1.0/WEB-INF/classes/:target/DD4-Biblical-1.0/WEB-INF/lib/*"
java $classpath com.digitald4.biblical.tools.CalendarValidator $date $2