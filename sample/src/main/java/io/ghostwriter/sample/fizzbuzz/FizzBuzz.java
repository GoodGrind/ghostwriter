package io.ghostwriter.sample.fizzbuzz;


/**
 * If you are not familiar with FizzBuzz check: http://en.wikipedia.org/wiki/Fizz_buzz
 */
public class FizzBuzz {

    private final int lowerBound;

    private final int upperBound;

    public static void main(String args[]) {
        System.out.println("Starting FizzBuzz application");
        if (args.length <= 1) {
            System.out.println("Please provide lower and upper bound parameters!");
            System.exit(0);
        }
        final int lowerBound = Integer.valueOf(args[0]);
        final int upperBound = Integer.valueOf(args[1]);
        final FizzBuzz fizzBuzz = new FizzBuzz(lowerBound, upperBound);
        fizzBuzz.execute();
        System.out.println("Finished running FizzBuzz application");
    }

    public FizzBuzz(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public void execute() {
        for (int i = lowerBound; i < upperBound; ++i) {
            String fizzBuzzValue = fizzBuzz(i);
            System.out.println("FizzBuzz value is: " + fizzBuzzValue);
        }
    }

    public String fizzBuzz(int num) {
        String result = "";
        if (isFizz(num)) {
            result += "Fizz";
        }
        else if (isBuzz(num)) {
            result += "Buzz";
        }
        else {
            result = String.valueOf(num);
        }

        return result;
    }

    public boolean isFizz(int num) {
        return num % 3 == 0;
    }

    public boolean isBuzz(int num) {
        return num % 5 == 0;
    }

}