package com.parkit.parkingsystem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

import junit.framework.Assert;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO = new ParkingSpotDAO();
    @Mock
    private static TicketDAO ticketDAO = new TicketDAO();

// attribut accessible partout dans la classe de test
     ArgumentCaptor <Ticket> ticketCaptor;
	 ArgumentCaptor <ParkingSpot> parkingSpotCaptor;
	 ArgumentCaptor <ParkingType> parkingTypeCaptor;

    @BeforeEach
    private void setUpPerTest() {
        try {
        	ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        	parkingSpotCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
            Mockito.lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // mockito.lenient a supprimer une fois les @parameterizedTest mis en place
       	 	Mockito.lenient().when(parkingSpotDAO.updateParking(parkingSpotCaptor.capture())).thenReturn(true);
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }
  /**
   * test la méthode d'entrée du véhicule
   */
    @Test
    public void processIncomingVehicleTest() {
    //Mock
    	when(inputReaderUtil.readSelection()).thenReturn(1); // mock de la saisie utilisateur du choix voiture (1) ou moto (2)
    	when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1); // mock du numéro de place de parking
    	when(ticketDAO.getNbTicket(any(Ticket.class))).thenReturn(2); // mock du nombre de fois que l'utilisateur est déjà rentré dans le parking

    // lancement metier
		parkingService.processIncomingVehicle();
    // verification que le traitement s'est bien passé
        verify(ticketDAO).saveTicket(ticketCaptor.capture());

        final Ticket ticketCaptured = ticketCaptor.getValue();
        final ParkingSpot parkingSpotCaptured = parkingSpotCaptor.getValue();
        ParkingSpot parkingSpotReference = new ParkingSpot(1, ParkingType.CAR, false);

        Assert.assertEquals("le parking doit être identique a l'attendu", parkingSpotReference, parkingSpotCaptured);
        Assert.assertNull("la date de sortie doit être null", ticketCaptured.getOutTime());
    }

/**
 * test de la méthode de sortie du véhicule
 */
    @Test
    public void processExitingVehicleTest(){
    	when(ticketDAO.updateTicket(ticketCaptor.capture())).thenReturn(true);

    	creationTicketMock();

    	Date dateAvantLancementMethode = new Date();
        parkingService.processExitingVehicle(new Date());
        Date dateApresLancementMethode = new Date();

        final Ticket ticketCaptured = ticketCaptor.getValue();
        final ParkingSpot parkingSpotCaptured = parkingSpotCaptor.getValue();

        Assert.assertTrue("le parking doit être vide", parkingSpotCaptured.isAvailable());
        Assert.assertTrue("la date de ma variable est avant la date de sortie du ticket", dateAvantLancementMethode.before(ticketCaptured.getOutTime()) || dateAvantLancementMethode.equals(ticketCaptured.getOutTime()));
        Assert.assertTrue("la date de ma variable est après la date de sortie du ticket", dateApresLancementMethode.after(ticketCaptured.getOutTime()) || dateApresLancementMethode.equals(ticketCaptured.getOutTime()));

    }

/**
 * mise a jour du ticket a faux la place de parking reste occupee
 */
     @Test
     public void processExitingVehicleTestUnableUpdate () {
    	 when(ticketDAO.updateTicket(ticketCaptor.capture())).thenReturn(false);
    	 creationTicketMock();
         parkingService.processExitingVehicle(new Date());
     	 verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
     }
/**
 * parkingSpot doit être disponible et avec un ID a 1
 */
     @Test
     public void getNextParkingNumberIfAvailableTest () {
    	when(inputReaderUtil.readSelection()).thenReturn(1); // mock de la saisie utilisateur du choix voiture (1) ou moto (2)
    	when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
    	ParkingSpot parkingSpotObtenu = parkingService.getNextParkingNumberIfAvailable();
    	Assert.assertTrue("le parking doit être vide", parkingSpotObtenu.isAvailable());
    	Assert.assertEquals("la place de parking attendue est la numero 1", 1, parkingSpotObtenu.getId());

     }
/**
 * résultat aucun spot disponible (la méthode renvoie null)
 */
     @Test
     public void getNextParkingNumberIfAvailableParkingNumberNotFoundTest () {
    	when(inputReaderUtil.readSelection()).thenReturn(1); // mock de la saisie utilisateur du choix voiture (1) ou moto (2)
     	when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(-1);
     	ParkingSpot parkingSpotObtenu = parkingService.getNextParkingNumberIfAvailable();
     	Assert.assertNull("aucun spot disponible", parkingSpotObtenu);

     }
/**
 * test de l'exception si l'utilisateur saisi autre chose que voiture ou moto
 */
     @Test
     public void getNextParkingNumberIfAvailableParkingNumberWrongArgumentTest() {
     	when(inputReaderUtil.readSelection()).thenReturn(3); // mock de la saisie utilisateur le choix 3 est un choix qui n'existe pas
     	ParkingSpot parkingSpotObtenu = parkingService.getNextParkingNumberIfAvailable();
     	verify(parkingSpotDAO, never()).getNextAvailableSlot(any(ParkingType.class));

     	Assert.assertNull("aucun spot disponible", parkingSpotObtenu);


     }
     public void creationTicketMock() {
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        when(ticketDAO.getTicket(any(String.class))).thenReturn(ticket);
     }
}
