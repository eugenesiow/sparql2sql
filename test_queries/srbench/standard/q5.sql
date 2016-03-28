
    SELECT
        'http://knoesis.wright.edu/ssw/System_K3HT' AS sensor 
    FROM
        _K3HT 
    WHERE
        _K3HT.time>'2013-05-08T13:00:00' 
        AND _K3HT.time<'2013-05-08T16:00:00' 
    GROUP BY
        'http://knoesis.wright.edu/ssw/System_K3HT' 
    HAVING
        avg(_K3HT.AirTemperature)<32.0 
        AND min(_K3HT.WindSpeed)>40.0 