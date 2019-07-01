//Version 2.0
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class ExprEvalApp   //driver class to control our expanded/inherited listener class
{
  public static void main(String[] args) throws IOException
  {
    System.out.println("** Expression Eval w/ antlr-listener **");
    Path input_path = Paths.get(args[0]);
    // Get lexer from input file
    ExprLexer lexer = new ExprLexer(CharStreams.fromPath(input_path));
    // Get a list of matched tokens
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    // Pass tokens to parser
    ExprParser parser = new ExprParser(tokens);
    // Walk parse-tree and attach our listener
    ParseTreeWalker walker = new ParseTreeWalker();
    EvalListener listener = new EvalListener();
    walker.walk(listener, parser.prog());
  }
}

/*Listener Class Extended*/
class EvalListener extends ExprBaseListener //inhertiance
{

  //Global Variables Used by almost all the funcitons:
  // hash-map for variables' integer value for assignment
  HashMap<String, String> variables = new HashMap<String, String>();
  //infix Vector
  Vector<String> infixVector = new Vector<String>();
  //postfix Vector
  Vector<String> postfixVector = new Vector<String>();


  //Check if an element is an operand(variable or Number)
  public int isVarorNum(String a)
  {
    int flag=-1;
    String MUL=Character.toString('*');
    String DIV=Character.toString('/');
    String ADD=Character.toString('+');
    String SUB=Character.toString('-');
    String EQU=Character.toString('=');
    String L_PAR=Character.toString('(');
    String R_PAR=Character.toString(')');

    if(a.equals(MUL)||a.equals(DIV)||a.equals(ADD)||a.equals(SUB)||a.equals(EQU)||a.equals(L_PAR)||a.equals(R_PAR)) //operator
    {
      flag=0;
    }
    else //operand
    {
      flag=1;
    }
    return flag;
  }

  //Check if an element is a variable
  public int isVar(String a)
  {
    int flag=-1;
    char checker=a.charAt(0);
    if(Character.isLetter(checker)) //first character of the string is a letter -> it's variable
    {
      return 1;
    }
    else
    {
      return 0;
    }
  }

  //Check the precedence of an operators
  public int Precedence(String op)
  {
    String MUL=Character.toString('*');
    String DIV=Character.toString('/');
    String ADD=Character.toString('+');
    String SUB=Character.toString('-');
    if(op.equals(MUL)||op.equals(DIV))
    {
      return 2;
    }
    else if(op.equals(ADD)||op.equals(SUB))
    {
      return 1;
    }
    else
    {
      return -1;
    }

  }

  //Fix the signs in the ingfix vector
  public void sign_fixer()
  {
    String equality=Character.toString('=');
    String plus=Character.toString('+');
    String minus=Character.toString('-');
    String division=Character.toString('/');
    String multiply=Character.toString('*');
    String l_paren=Character.toString('(');
    for(int index=0; index < infixVector.size()-2; index++)
    {
      String item=infixVector.get(index);
      String next_item=infixVector.get(index+1);
      String number=infixVector.get(index+2);
      if((item.equals(equality)||item.equals(division)||item.equals(multiply)||item.equals(l_paren))&&(next_item.equals(plus)||next_item.equals(minus))) //=+5;  ..*+5    ../+5
      {
        //change the number
        infixVector.set(index+2,(next_item+number));

        //remove the left sign
        infixVector.remove(index+1);

        index--; //since you removed on element
      }
      else if(item.equals(plus)&&(next_item.equals(plus)||next_item.equals(minus))) //....++   ....+-
      {
          infixVector.remove(index);

          index--;

      }
      else if(item.equals(minus)&&next_item.equals(plus)) //....-+
      {
          infixVector.remove(index+1);
          index--;
      }
      else if(item.equals(minus)&&next_item.equals(minus)) //....--
      {
          infixVector.set(index,plus);
          infixVector.remove(index+1);
          index--;
      }
      else if(index==0&&(item.equals(plus)||item.equals(minus)))   //first element is the sign
      {
          infixVector.set(index+1,(item+next_item));
          infixVector.remove(index);
          index--;
      }

    }

  }


  //Check if it's an assignment function
  public int check_assignment()
  {
    String equality=Character.toString('=');
    int flag=0; //flag=0 -> Not an assignment //flag=1 -> assignment

    for(int index=0; index < infixVector.size(); index++)
    {
      String checker=infixVector.get(index);
      if(checker.equals(equality))  //assignment found
      {
        flag=1;
        break;
      }
      else
      {
        flag=0;
      }
    }
    return flag;
  }

  //Change infixVector to postfixVector
  public void inifx_to_postfix()
  {
    //temp stack for postfix conversion
    Stack<String> tempStack = new Stack<String>();//stack

    //scan the expression left to right + Apply shunting yard algorithm
    for(int index=0; index < infixVector.size(); index++)
    {
      String infix_item = infixVector.get(index); //current item in the infix vector

      String left_paren=Character.toString('(');
      String Right_paren=Character.toString(')');


      // If the scanned vector item is an operand, add it to output (postfixVector).
      if (isVarorNum(infix_item)==1)
      {
        postfixVector.addElement(infix_item);
      }
      else if (infix_item.equals(left_paren)) //if left parenthesis push to stack
      {
        tempStack.push(infix_item);
      }
      // pop until an '(' is encountered. e.g. ( * ) -> add * to postfixvector
      else if (infix_item.equals(Right_paren))
      {
        while (!tempStack.isEmpty()&&!(tempStack.peek()).equals(left_paren))
        {
          postfixVector.addElement((tempStack.pop()));
        }
        tempStack.pop(); //pop the last left parenthesis
      }
      else // + - * / case
      {
        while (!tempStack.isEmpty() && Precedence(infix_item) <= Precedence(tempStack.peek()))  //pop the lower or equal operators
        {
          postfixVector.addElement((tempStack.pop()));
        }
        tempStack.push(infix_item);
      }

    }
    // pop all the operators from the stack
    while (!tempStack.isEmpty())
    {
      postfixVector.addElement((tempStack.pop()));
    }
  }


  //replace variables in postfix Vector with their values from HashMap
  public void replacevars()
  {
    for(int index=0; index < postfixVector.size(); index++)  //scanning the postfix order
    {
      String postfix_item = postfixVector.get(index);
      if(isVar(postfix_item)==1) //is variable -> get its value from the hash map
      {
        String value=variables.get(postfix_item);
        postfixVector.setElementAt(value, index);
      }
    }
  }

  //Final postfix evaluation
  public String evaluate_postfix()
  {
    String MUL=Character.toString('*');
    String DIV=Character.toString('/');
    String ADD=Character.toString('+');
    String SUB=Character.toString('-');
    //temp stack for storing variables
    Stack<String> tempStack = new Stack<String>();//stack

    for(int index=0; index < postfixVector.size(); index++)  //scanning the postfix order
    {
      String postfix_item = postfixVector.get(index);

      if (isVarorNum(postfix_item)==1)  //if number push to stack
      {
        tempStack.push(postfix_item);
      }
      else //operator found
      {
        String op2 = tempStack.pop();
        String op1 = tempStack.pop();
        String operator = postfix_item;
        if(operator.equals(ADD))
        {
          double OP2 = Double.parseDouble(op2);
          double OP1 = Double.parseDouble(op1);
          double answer=OP2+OP1;
          tempStack.push(Double.toString(answer));
        }
        else if(operator.equals(SUB))
        {
          double OP2 = Double.parseDouble(op2);
          double OP1 = Double.parseDouble(op1);
          double answer=OP1-OP2;
          tempStack.push(Double.toString(answer));
        }
        else if(operator.equals(MUL))
        {
          double OP2 = Double.parseDouble(op2);
          double OP1 = Double.parseDouble(op1);
          double answer=OP1*OP2;
          tempStack.push(Double.toString(answer));

        }
        else //division
        {
          double OP2 = Double.parseDouble(op2);
          double OP1 = Double.parseDouble(op1);
          double answer=OP1/OP2;
          tempStack.push(Double.toString(answer));
        }


      }
    }
    return tempStack.pop();
  }

  //This Function Pushes nodes/terminals into an infixvector (infix order) and then calls other functions -MAIN
  @Override
  public void visitTerminal(TerminalNode node)
  {

    String semicolon=Character.toString(';');
    String new_l=Character.toString('\n');
    String terminal_node=node.getText();
    //push terminals into stack -> infix order building -> until you reach ';' (one statement)
    if(terminal_node.equals(semicolon)) //end of statement -> evaluation should start
    {
      //fix the extra plusses if any
      //fix the infix vector cases for any sign
      sign_fixer();
      //check whether is an assignment or expression
      int checker=check_assignment();
      //if assignment add to hashmap and clear the infix vector
      if(checker==1)//assignment statement
      {
        //adding to hashmap
        String variable=infixVector.get(0);
        String value=infixVector.get(2);
        variables.put(variable,value);

        //clear the infix vector
        infixVector.clear();
        checker=0;
      }
      //else : 1)infix_to_postfix   2)postfix_evaluation   3)clear postfix/infix vector
      else
      {
        //step 1: convert the infix to postfix
        inifx_to_postfix();
        //step 2: Replace Varibale with their values
        replacevars();
        //Step 3: postfix_evaluation
        System.out.println(evaluate_postfix());
        //clear vectors for next expression
        infixVector.clear();
        postfixVector.clear();
      }


    }
    else if(terminal_node.equals(new_l))
    {
      //do nothing -> don't add to the infix_vector nor evaluate
    }
    else //still in statement
    {
      String temp=node.getText();
      //adding terminals to the infix vector
      infixVector.addElement(temp);
    }

  }

}
