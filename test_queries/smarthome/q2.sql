
    SELECT
        max(environment.insideTemp) AS max ,
        min(environment.insideTemp) AS min ,
        day(environment.TimestampUTC) AS day 
    FROM
        environment 
    WHERE
        environment.TimestampUTC>'2012-06-01T00:00:00' 
        AND environment.TimestampUTC<'2012-07-01T00:00:00' 
    GROUP BY
        day(environment.TimestampUTC)  