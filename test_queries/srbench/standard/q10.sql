
    SELECT
        DISTINCT '46.43333' AS lat ,
        '-109.83333' AS long ,
        '4160.0' AS alt 
    FROM
        _K3HT 
    WHERE
        _K3HT.time>'2003-04-01T00:00:00' 
        AND _K3HT.time<'2013-04-02T00:00:00'   