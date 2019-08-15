import java.util.concurrent.BlockingQueue;

public class MessageQueuePrinter implements Runnable {

    Client c;
    BlockingQueue messageQueue;
    MessagingGUI mg;

    MessageQueuePrinter(Client c, MessagingGUI mg){
        this.c = c;
        this.mg = mg;
        this.messageQueue = c.getMessageQueue();

    }

    MessageQueuePrinter(Client c){
        this.c = c;
        this.messageQueue = c.getMessageQueue();
    }

    @Override
    public void run() {
        while(c.isOnline()){
            try {
                if(mg != null) {
                    mg.appendTextArea((String) messageQueue.take());
                }
                else{
                    System.out.println((String) messageQueue.take());
                }
            }
            catch(Exception e){
                System.out.println(e);
            }
        }
    }
}
