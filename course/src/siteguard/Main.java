package siteguard;

import java.awt.EventQueue;
import java.io.File;

import javax.swing.JOptionPane;

import siteguard.exceptions.StorageException;
import siteguard.gui.MainFrame;
import siteguard.service.MonitorService;
import siteguard.storage.IncidentStore;
import siteguard.storage.MemoryIncidentStore;
import siteguard.storage.StoreFactory;

/**
 * Starts SiteGuard. Data is kept in the "data" folder next to where the program runs.
 */
public class Main {

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                MonitorService monitor;
                try {
                    IncidentStore store = StoreFactory.open(new File("data"));
                    monitor = new MonitorService(store);
                } catch (StorageException e) {
                    JOptionPane.showMessageDialog(null, "Saved incidents could not be loaded:\n" + e.getMessage()
                            + "\n\nSiteGuard will run, but history is TEMPORARY.");
                    try {
                        monitor = new MonitorService(new MemoryIncidentStore());
                    } catch (StorageException never) {
                        return;
                    }
                }
                MainFrame frame = new MainFrame(monitor);
                frame.setVisible(true);
            }
        });
    }
}
