<?php

namespace App\Contracts;

interface ProvidesProductProperties
{
    /**
     * @return Properties<\App\Models\Product>
     */
    public function getProductProperties(): array;
}
