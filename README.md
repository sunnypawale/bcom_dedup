# bcom_dedup
 
 
 
### Notes
    - Apllcation will always create new H2 Embedded Database, if you dont want to create new DB use -DisNewDb="false" at start of application
    - As this applcation play with large amount of data, please use 50 to 100 hotel Ids only
    - We store MD5 hash for availability block for unique key as hotelid_checkin_los_blcokid_channel
    
# Build and Run

### sbt assembly
        - This wil give you a executable jar.
### java  -jar bcom-dedup-app-1.0-all.jar
        - To run the application with all default values.
        - To override value please use below commond.
### java -Dhotels="1,2,3,4" -jar bcom-dedup-app-1.0-all.jar
        - With overriden values
#### Fields available to override
        1. 'bcom.url': Client URl
            Default value = "https://xml-avchanges.booking.com"
        2. 'username'
        3. 'pass'
        4. 'hotels': Whitelist hotelIds, max 100
            e.g -Dhotels="1,2,3,4"
        5. 'isNewDb': This paramter to create new embdded db file in local server(Delete old and create new)
            e.g -DisNewDb="true" or ("true" = Default value)
        6. 'is.raw.data' to log actual raw availability 
            
            
### How to Query H2 DB.
    - Download h2-1.4.182.jar and run the jar file in same folder as 'data', which will start Web client for H2
    - Url to H2 Web cleint: http://localhost:8082/ 
    - Once we open above page enter following details.
        Driver class: org.h2.Driver
        Jdbc url: jdbc:h2:./data/local/dedup
#### Queries

       - (1) select count(*) from dedup
                : Gives total number of updates received
       - (2) select count(*) from dedup where is_dedup = true
                : Gives total number of duplicate records
       - select key, hash, count(*) as count from dedup where is_dedup= true group by key, hash order by count desc
                : Gives count of duplicate for given unique key
        
       - select * from dedup where key = '1310132_2018-02-17_10_131013202_105280564_1_1_0'
       
#### Table Structure:
        - CREATE TABLE IF NOT EXISTS DeDup(id bigint auto_increment, key varchar(60), hash BINARY(16), from_date DATE, to_date DATE, cd_from_time varchar(30),  cd_to_time varchar(30), is_dedup BOOLEAN, avail varchar(1000))

      
### How to calculate Percentage of duplicates
    - (Result of Query (2)/Result of Query (1)) * 100
    
    

        



    
       