package org.mycore.oai;

public class MCROAILuceneResult implements MCROAIResult {

    protected MCROAICombinedResult result;

    public MCROAILuceneResult(MCROAICombinedResult result) {
        this.result = result;
    }

    @Override
    public int getNumHits() {
        return this.getResult().size();
    }

    @Override
    public String getID(int cursor) {
        return this.getResult().getHit(cursor).getID();
    }

    public MCROAICombinedResult getResult() {
        return result;
    }

}
