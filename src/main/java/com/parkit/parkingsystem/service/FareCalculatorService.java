package com.parkit.parkingsystem.service;

import java.util.concurrent.TimeUnit;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        TimeUnit time = TimeUnit.HOURS;
        long duration = time.convert(outHour - inHour, TimeUnit.MILLISECONDS);

    	double prixVehiculeEnCoursPourUneHeure;

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
            	prixVehiculeEnCoursPourUneHeure = Fare.CAR_RATE_PER_HOUR;
                break;
            }
            case BIKE: {
            	prixVehiculeEnCoursPourUneHeure = Fare.BIKE_RATE_PER_HOUR;
                break;
            }

            default: throw new IllegalArgumentException("Unkown Parking Type");
        }

        if (duration == 0) { // le véhicule reste moins d'une heure
        	ticket.setPrice(prixVehiculeEnCoursPourUneHeure * 0.75);

        }else { // le véhicule reste plus d'une heure
        	ticket.setPrice(duration * prixVehiculeEnCoursPourUneHeure);
        }

    }
}