
    SELECT
        motiondate ,
        motionhours ,
        motionplatform ,
        power ,
        meter ,
        name      
    FROM
        (SELECT
            CONCAT('http://iot.soton.ac.uk/smarthome/platform#',
            sensors.Location) AS meterplatform ,
            HOUR(meter.TimestampUTC) AS meterhours ,
            CAST(meter.TimestampUTC AS DATE) AS meterdate ,
            avg(meter.RealPowerWatts) AS power ,
            CONCAT('http://iot.soton.ac.uk/smarthome/sensor#',
            sensors.SensingDevice) AS meter ,
            MAX(sensors.Label) AS name 
        FROM
            sensors ,
            meter 
        WHERE
            meter.MeterName=sensors.SensingDevice 
            AND meter.RealPowerWatts>0 
            AND meter.TimestampUTC>'2012-07-01T00:00:00' 
            AND meter.TimestampUTC<'2012-07-07T00:00:00' 
        GROUP BY
            CAST(meter.TimestampUTC AS DATE) ,
            HOUR(meter.TimestampUTC) ,
            CONCAT('http://iot.soton.ac.uk/smarthome/platform#',
            sensors.Location) ,
            CONCAT('http://iot.soton.ac.uk/smarthome/sensor#',
            sensors.SensingDevice)  )  ,
        (SELECT
            sum(motion.MotionOrNoMotion) AS isMotion ,
            CONCAT('http://iot.soton.ac.uk/smarthome/platform#',
            sensors.Location) AS motionplatform ,
            HOUR(motion.TimestampUTC) AS motionhours ,
            CAST(motion.TimestampUTC AS DATE) AS motiondate    
        FROM
            sensors ,
            motion 
        WHERE
            sensors.SensingDevice=motion.MotionSensorName 
            AND meter.TimestampUTC>'2012-07-01T00:00:00' 
            AND meter.TimestampUTC<'2012-07-07T00:00:00' 
        GROUP BY
            CAST(motion.TimestampUTC AS DATE) ,
            HOUR(motion.TimestampUTC) ,
            CONCAT('http://iot.soton.ac.uk/smarthome/platform#',
            sensors.Location)  )  
    WHERE
        motionplatform=meterplatform 
        AND motionhours=meterhours 
        AND motiondate=meterdate 
        AND isMotion=0   