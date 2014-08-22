FROM java:8
MAINTAINER tjdziuba@neomailbox.ch
ADD target/uberjar/minisas-0.1.0-SNAPSHOT-standalone.jar /app.jar
EXPOSE 8080
CMD java -jar /app.jar
