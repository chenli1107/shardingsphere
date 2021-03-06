/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.sql.parser.sql.common.util;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.sql.parser.sql.common.constant.LogicalOperator;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.BinaryOperationExpression;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.predicate.AndPredicate;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.predicate.OrPredicateSegment;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

/**
 * Expression builder.
 */
@RequiredArgsConstructor
public final class ExpressionBuilder {
    
    private final ExpressionSegment expression;
    
    /**
     * Extract and predicates.
     *
     * @return Or predicate segment
     */
    public OrPredicateSegment extractAndPredicates() {
        OrPredicateSegment result = new OrPredicateSegment();
        if (expression instanceof BinaryOperationExpression) {
            String operator = ((BinaryOperationExpression) expression).getOperator();
            Optional<LogicalOperator> logicalOperator = LogicalOperator.valueFrom(operator);
            if (logicalOperator.isPresent() && LogicalOperator.OR == logicalOperator.get()) {
                ExpressionBuilder leftBuilder = new ExpressionBuilder(((BinaryOperationExpression) expression).getLeft());
                ExpressionBuilder rightBuilder = new ExpressionBuilder(((BinaryOperationExpression) expression).getRight());
                result.getAndPredicates().addAll(leftBuilder.extractAndPredicates().getAndPredicates());
                result.getAndPredicates().addAll(rightBuilder.extractAndPredicates().getAndPredicates());
            } else if (logicalOperator.isPresent() && LogicalOperator.AND == logicalOperator.get()) {
                result.getAndPredicates().addAll(createAndPredicates());
            } else {
                AndPredicate andPredicate = new AndPredicate();
                andPredicate.getPredicates().add(expression);
                result.getAndPredicates().add(andPredicate);
            }
        } else {
            AndPredicate andPredicate = new AndPredicate();
            andPredicate.getPredicates().add(expression);
            result.getAndPredicates().add(andPredicate);
        }
        return result;
    }
    
    private AndPredicate createAndPredicate(final AndPredicate left, final AndPredicate right) {
        AndPredicate result = new AndPredicate();
        result.getPredicates().addAll(left.getPredicates());
        result.getPredicates().addAll(right.getPredicates());
        return result;
    }

    private Collection<AndPredicate> createAndPredicates() {
        Collection<AndPredicate> result = new LinkedList<>();
        ExpressionBuilder leftBuilder = new ExpressionBuilder(((BinaryOperationExpression) expression).getLeft());
        ExpressionBuilder rightBuilder = new ExpressionBuilder(((BinaryOperationExpression) expression).getRight());
        ExpressionSegment leftExpression = ((BinaryOperationExpression) expression).getLeft();
        if (leftExpression instanceof BinaryOperationExpression) {
            String leftOp = ((BinaryOperationExpression) leftExpression).getOperator();
            Optional<LogicalOperator> logicLeftOp = LogicalOperator.valueFrom(leftOp);
            if (logicLeftOp.isPresent() && LogicalOperator.OR == logicLeftOp.get()) {
                ExpressionBuilder beforeOrBuilder = new ExpressionBuilder(((BinaryOperationExpression) leftExpression).getLeft());
                ExpressionBuilder afterOrBuilder = new ExpressionBuilder(((BinaryOperationExpression) leftExpression).getRight());
                result.addAll(beforeOrBuilder.extractAndPredicates().getAndPredicates());
                result.addAll(createResultCollection(afterOrBuilder, rightBuilder));
                return result;
            }
        }
        result.addAll(createResultCollection(leftBuilder, rightBuilder));
        return result;
    }

    private Collection<AndPredicate> createResultCollection(final ExpressionBuilder leftBuilder, final ExpressionBuilder rightBuilder) {
        Collection<AndPredicate> result = new LinkedList<>();
        for (AndPredicate eachLeft : leftBuilder.extractAndPredicates().getAndPredicates()) {
            for (AndPredicate eachRight : rightBuilder.extractAndPredicates().getAndPredicates()) {
                result.add(createAndPredicate(eachLeft, eachRight));
            }
        }
        return result;
    }
}
