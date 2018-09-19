package elements;

import java.util.HashMap;
import java.util.Map;

public class Scenario {

    private String inputFilesName;
    private String objective;
    private String useCase;
    private Map<String, Boolean> constraints;
    private boolean maximization;

    public Scenario(){
        constraints = new HashMap<>();
    }

    public String getInputFilesName() {
        return inputFilesName;
    }

    public void setInputFilesName(String inputFilesName) {
        this.inputFilesName = inputFilesName;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getUseCase() {
        return useCase;
    }

    public void setUseCase(String useCase) {
        this.useCase = useCase;
    }

    public Map<String, Boolean> getConstraints() {
        return constraints;
    }

    public void setConstraints(Map<String, Boolean> constraints) {
        this.constraints = constraints;
    }

    public boolean isMaximization() {
        return maximization;
    }

    public void setMaximization(boolean maximization) {
        this.maximization = maximization;
    }
}
