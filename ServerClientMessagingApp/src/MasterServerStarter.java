public class MasterServerStarter implements Runnable {
    MasterServer ms;
    public MasterServerStarter(MasterServer ms){
        this.ms = ms;
    }

    @Override
    public void run() {
        try{
            ms.startMasterServer();
        }
        catch (Exception e){
            System.out.println(e);
        }

    }
}
