/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;

import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FunctionOffsetPostScript extends GhidraScript {

    public void run() throws Exception {
        String spaceSeparatedFunctionNames = propertiesFileParams.getValue("functionNames");
        List<String> listOfFunctions = Arrays.asList(spaceSeparatedFunctionNames.split("\\s+"));
        List<BigInteger> output = new ArrayList<>();

        // Find the function offsets
        for (String function : listOfFunctions) {
            FunctionIterator functionIterator = currentProgram.getListing().getFunctions(true);
            BigInteger offset = null;
            while (functionIterator.hasNext()) {
                Function nextFunction = functionIterator.next();
                if (!nextFunction.getName().equals(function)) {
                    continue; // Skip to the next iteration if the function name doesn't match
                }

                // If the function name matches, calculate the offset
                offset =
                        nextFunction
                                .getEntryPoint()
                                .subtract(currentProgram.getImageBase().getOffset())
                                .getOffsetAsBigInteger();
                break;
            }

            // 'output' is appended in the same order as 'listOfFunctions' contains the function
            // names. If an offset is not found, null is appended.
            output.add(offset);
        }
        try (Socket socket =
                        new Socket(
                                "localhost",
                                Integer.parseInt(propertiesFileParams.getValue("port")));
                ObjectOutputStream outputStream =
                        new ObjectOutputStream(socket.getOutputStream()); ) {
            outputStream.writeObject(output);
        }
    }
}
