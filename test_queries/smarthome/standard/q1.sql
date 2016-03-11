
    SELECT
        avg(environment.insideTemp) AS sval ,
        HOUR(environment.TimestampUTC) AS hours 
    FROM
        environment 
    WHERE
        environment.TimestampUTC>'2012-07-20T00:00:00' 
        AND environment.TimestampUTC<'2012-07-21T00:00:00' 
    GROUP BY
        HOUR(environment.TimestampUTC)  