
    SELECT
        CASE  
            WHEN avg(_4UT01.WindSpeed) < 1 THEN 0 
            WHEN avg(_4UT01.WindSpeed) < 4 THEN 1 
            WHEN avg(_4UT01.WindSpeed) < 8 THEN 2 
            WHEN avg(_4UT01.WindSpeed) < 13 THEN 3 
            WHEN avg(_4UT01.WindSpeed) < 18 THEN 4 
            WHEN avg(_4UT01.WindSpeed) < 25 THEN 5 
            WHEN avg(_4UT01.WindSpeed) < 31 THEN 6 
            WHEN avg(_4UT01.WindSpeed) < 39 THEN 7 
            WHEN avg(_4UT01.WindSpeed) < 47 THEN 8 
            WHEN avg(_4UT01.WindSpeed) < 55 THEN 9 
            WHEN avg(_4UT01.WindSpeed) < 64 THEN 10 
            WHEN avg(_4UT01.WindSpeed) < 73 THEN 11 
            ELSE 12  
        END AS windForce ,
        avg(_4UT01.WindDirection) AS avgWindDirection 
    FROM
        _4UT01 
    WHERE
        _4UT01.time>'2003-04-01T00:00:00' 
        AND _4UT01.time<'2003-04-02T00:00:00'   